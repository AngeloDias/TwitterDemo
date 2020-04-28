package br.com.training.android.twitterdemo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_ticket.view.*
import kotlinx.android.synthetic.main.tweets_ticket.view.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private var listTweets = ArrayList<Ticket>()
    private var adapter: MyTweetAdapter? = null
    private val _pickImageCode = 156
    private var myEmail: String? = null
    private val _firebaseStorageFolderPath = "gs://tictactoeudemy-f001e.appspot.com"
    private var downloadURL: String? = null
    private var myRef: DatabaseReference? = null
    private var userUID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bundle: Bundle = intent.extras!!
        myEmail = bundle.getString("email")
        userUID = bundle.getString("uid")
        myRef = FirebaseDatabase.getInstance().reference

        //Dummy data
        listTweets.add(Ticket("0", "Hello", "URL", "add"))

        adapter = MyTweetAdapter(applicationContext, listTweets)
        listViewTweets.adapter = adapter

        loadPost()
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

            uploadImage(BitmapFactory.decodeFile(picturePath))
        }
    }

    private fun uploadImage(bitmap: Bitmap) {
        listTweets.add(0, Ticket("0", "Hello", "URL", "loading"))
        adapter!!.notifyDataSetChanged()

        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.getReferenceFromUrl(_firebaseStorageFolderPath)
        val dateFormat = SimpleDateFormat("ddMMyyHHmmss")
        val dataObj = Date()
        val emailStr = myEmail.toString().split("@")[0]
        val imgPath = "$emailStr.${dateFormat.format(dataObj)}.jpg"
        val imgRef = storageReference.child("imagesPost/$imgPath")
        val byteAOStream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteAOStream)

        val data = byteAOStream.toByteArray()
        val uploadTask = imgRef.putBytes(data)

        uploadTask.addOnFailureListener {
            Toast.makeText(this, "Fail to upload image", Toast.LENGTH_LONG).show()
        }.addOnSuccessListener {taskSnapshot ->
            downloadURL = taskSnapshot.storage.downloadUrl.toString()

            listTweets.removeAt(0)
            adapter!!.notifyDataSetChanged()

        }
    }

    private fun loadPost() {
        myRef!!.child("posts").addValueEventListener(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    val tData = dataSnapshot.value as HashMap<String, Any>

                    for(key in tData.keys) {
                        val post = tData[key] as HashMap<String, Any>

                        listTweets.add(Ticket(key, post["text"] as String, post["postImage"] as String,
                            post["userUID"] as String))
                    }

                    adapter!!.notifyDataSetChanged()

                } catch (exc: Exception){}
            }

        })
    }

    inner class MyTweetAdapter(context: Context, var listTicketsAdapter: ArrayList<Ticket>) :
        BaseAdapter() {
        var context: Context?= context

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            val myTweet = listTicketsAdapter[p0]

            if(myTweet.tweetPersonUID == "add") {
                val myView = layoutInflater.inflate(R.layout.add_ticket, null)

                myView.imgViewAttach.setOnClickListener {
                    loadImage()
                }

                myView.imgViewPost.setOnClickListener {
                    //upload server
                    myRef!!.child("posts").push().setValue(
                        PostInfo(userUID!!,
                            myView.editTxtPost.text.toString(), downloadURL!!))

                    myView.editTxtPost.setText("")
                }

                return myView
            } else if(myTweet.tweetPersonUID == "loading"){

                return layoutInflater.inflate(R.layout.loading_ticket,null)
            } else if(myTweet.tweetPersonUID == "ads"){
                val myView=layoutInflater.inflate(R.layout.ads_ticket,null)
                val mAdView = myView.findViewById(R.id.adView) as AdView
                val adRequest = AdRequest.Builder().build()

                mAdView.loadAd(adRequest)

                return myView
            }else{
                val myView = layoutInflater.inflate(R.layout.tweets_ticket,null)
                myView.txtViewTweet.text = myTweet.tweetText

//                Picasso.with(context).load(myTweet.tweetImageURL).into(myView.tweetPicture)

                myRef!!.child("users").child(myTweet.tweetPersonUID)
                    .addValueEventListener(object : ValueEventListener {

                        override fun onDataChange(dataSnapshot: DataSnapshot) {

                            try {
                                val td = dataSnapshot.value as HashMap<String,Any>

                                for(key in td.keys){
                                    val userInfo = td[key] as String

                                    if(key == "ProfileImage"){
//                                        Picasso.with(context).load(userInfo).into(myView.picturePath)
                                    }else{
                                        myView.txtUserName.text = userInfo
                                    }
                                }

                            }catch (ex:Exception){}

                        }

                        override fun onCancelled(p0: DatabaseError) {}

                    })

                return myView
            }
        }

        override fun getItem(p0: Int): Any {
            return listTicketsAdapter[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getCount(): Int {
            return listTicketsAdapter.size
        }
    }

}
