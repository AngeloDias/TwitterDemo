package br.com.training.android.twitterdemo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_login.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : AppCompatActivity() {
    private val _readImage: Int = 253
    private val _pickImageCode = 156
    private var mAuth: FirebaseAuth? = null
    private val _firebaseStorageFolderPath = "gs://tictactoeudemy-f001e.appspot.com"
    private var firebaseDB: FirebaseDatabase? = null
    private var myDBRef: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        firebaseDB = FirebaseDatabase.getInstance()
        myDBRef = firebaseDB!!.reference

        imgViewPerson.setOnClickListener {
            checkPermission()
        }

        FirebaseMessaging.getInstance().subscribeToTopic("news")
    }

    private fun loginToFirebase(email: String, pass: String) {
        mAuth!!.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) {task ->

            if(task.isSuccessful) {
                Toast.makeText(applicationContext, "Successful login", Toast.LENGTH_LONG).show()

                saveImageInFirebase()
            } else {
                Toast.makeText(applicationContext, "Login failed", Toast.LENGTH_LONG).show()
                task.exception?.printStackTrace()
            }
        }.addOnFailureListener { exception ->
            exception.printStackTrace()
        }
    }

    private fun saveImageInFirebase() {
        val currentUser = mAuth!!.currentUser
        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.getReferenceFromUrl(_firebaseStorageFolderPath)
        val dateFormat = SimpleDateFormat("ddMMyyHHmmss")
        val dataObj = Date()
        val emailStr = currentUser!!.email!!.toString().split("@")[0]
        val imgPath = "$emailStr.${dateFormat.format(dataObj)}.jpg"
        val imgRef = storageReference.child("images/$imgPath")

        imgViewPerson.isDrawingCacheEnabled = true
        imgViewPerson.buildDrawingCache()

        val drawable = imgViewPerson.drawable as BitmapDrawable
        val bitmap = drawable.bitmap
        val byteAOStream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteAOStream)

        val data = byteAOStream.toByteArray()
        val uploadTask = imgRef.putBytes(data)

        uploadTask.addOnFailureListener {
            Toast.makeText(this, "Fail to upload image", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener {
            myDBRef!!.child("users").child(currentUser.uid).child("email").setValue(currentUser.email)
            myDBRef!!.child("users").child(currentUser.uid).child("profileImage").setValue(currentUser.email)

            loadTweets()
        }
    }

    override fun onStart() {
        super.onStart()

        loadTweets()
    }

    private fun loadTweets() {
        val currentUser = mAuth!!.currentUser

        if(currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)

            intent.putExtra("email", currentUser.email)
            intent.putExtra("uid", currentUser.uid)
            startActivity(intent)
        }
    }

    private fun checkPermission() {
        if(Build.VERSION.SDK_INT >= 23) {
            if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), _readImage)

                return
            }
        }

        loadImage()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            _readImage ->
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadImage()
                } else {
                    Toast.makeText(this, "Can't access your images", Toast.LENGTH_LONG).show()
                }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun loadImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        startActivityForResult(intent, _pickImageCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == _pickImageCode && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(selectedImage!!, filePathColumn, null, null, null)

            cursor!!.moveToFirst()

            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)

            cursor.close()

            imgViewPerson.setImageBitmap(BitmapFactory.decodeFile(picturePath))
        }
    }

    fun onLogin(view: View) {
        loginToFirebase(editTextEmailLogin.text.toString(), editTextPassLogin.text.toString())
    }

}
