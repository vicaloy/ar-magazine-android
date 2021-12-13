package com.example.magazine

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.ux.ArFragment
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.google.ar.sceneform.rendering.ViewRenderable
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.magazine.RecyclerViewAdapter.OnItemClickListener
import com.google.ar.core.*
import com.google.ar.core.Camera
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Quaternion
import kotlinx.android.synthetic.main.item_detail.view.*
import java.io.IOException
import java.lang.Exception
import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    var mSession: Session? = null
    private var arFragment: ArFragment? = null
    private var arSceneView: ArSceneView? = null
    private var modelAdded = false
    private var sessionConfigured = false
    private val lipstickList: MutableList<Drawable> = ArrayList()
    private val eyeList: MutableList<Drawable> = ArrayList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadLists()

        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment?

        arFragment?.planeDiscoveryController?.hide()
        arFragment?.planeDiscoveryController?.setInstructionView(null)
        arFragment?.arSceneView?.planeRenderer?.isEnabled = false
        arFragment?.arSceneView?.isLightEstimationEnabled = false

        arFragment?.arSceneView?.scene?.addOnUpdateListener { frameTime: FrameTime ->
            onUpdateFrame(
                frameTime
            )
        }
        arSceneView = arFragment?.arSceneView
    }

    private fun loadLists(){

        lipstickList.add(resources.getDrawable(R.drawable.lipstick_1))
        lipstickList.add(resources.getDrawable(R.drawable.lipstick_2))
        lipstickList.add(resources.getDrawable(R.drawable.lipstick_3))
        lipstickList.add(resources.getDrawable(R.drawable.lipstick_4))
        lipstickList.add(resources.getDrawable(R.drawable.lipstick_5))
        lipstickList.add(resources.getDrawable(R.drawable.lipstick_6))
        lipstickList.add(resources.getDrawable(R.drawable.lipstick_7))
        lipstickList.add(resources.getDrawable(R.drawable.lipstick_8))

        eyeList.add(resources.getDrawable(R.drawable.eye_1))
        eyeList.add(resources.getDrawable(R.drawable.eye_2))
        eyeList.add(resources.getDrawable(R.drawable.eye_3))
        eyeList.add(resources.getDrawable(R.drawable.eye_4))
        eyeList.add(resources.getDrawable(R.drawable.eye_5))
        eyeList.add(resources.getDrawable(R.drawable.eye_6))
    }

    private fun setupAugmentedImageDb(config: Config): Boolean {
        val augmentedImageBitmap = loadAugmentedImage() ?: return false
        val augmentedImageDatabase = AugmentedImageDatabase(mSession)
        augmentedImageDatabase.addImage("image", augmentedImageBitmap)
        config.augmentedImageDatabase = augmentedImageDatabase
        return true
    }

    private fun loadAugmentedImage(): Bitmap? {
        try {
            assets.open("makeup.png").use { `is` -> return BitmapFactory.decodeStream(`is`) }
        } catch (e: IOException) {
            Log.e("ImageLoad", "IO Exception while loading", e)
        }
        return null
    }

    private fun removeNodes(){
        val children: List<Node> = ArrayList(arFragment!!.arSceneView.scene.children)
        for (node in children) {
            if (node is AnchorNode) {
                if (node.anchor != null) {
                    node.anchor!!.detach()
                }
            }
        }
    }
    private fun onUpdateFrame(frameTime: FrameTime) {
        val frame = arFragment?.arSceneView?.arFrame
        val augmentedImages = frame?.getUpdatedTrackables(
            AugmentedImage::class.java
        )
        if(augmentedImages!=null){
            for (augmentedImage in augmentedImages) {
                if (augmentedImage.trackingState == TrackingState.TRACKING && augmentedImage.trackingMethod == AugmentedImage.TrackingMethod.FULL_TRACKING) {
                    if (augmentedImage.name.contains("image") && !modelAdded) {
                        renderObject(augmentedImage.createAnchor(augmentedImage.centerPose), lipstickList, true)
                        renderObject(augmentedImage.createAnchor(augmentedImage.centerPose), eyeList, false)
                        modelAdded = true
                    }
                }else{
                    removeNodes()
                    modelAdded = false
                }
            }
        }

    }

    private fun renderObject(anchor: Anchor, drawableList: MutableList<Drawable>, isLipstick: Boolean) {

        ViewRenderable.builder()
            .setView(arFragment?.context, R.layout.recyclerview_control)
            .build()
            .thenAccept { 
                val view = it.view
                val recyclerView: RecyclerView = view.findViewById(R.id.list)
                val mLayoutManager = LinearLayoutManager(arFragment?.context)
                recyclerView.layoutManager = mLayoutManager
                recyclerView.adapter = RecyclerViewAdapter(drawableList, object:OnItemClickListener{
                    override fun onItemClick(item: Drawable?) {
                        findViewById<ConstraintLayout>(R.id.detail).visibility = View.VISIBLE
                        findViewById<ConstraintLayout>(R.id.detail).appCompatImageView.setImageDrawable(item)
                        findViewById<ConstraintLayout>(R.id.detail).setOnTouchListener(object: OnSwipeTouchListener(applicationContext){
                            override fun onSwipeBottom(){
                                (findViewById<ConstraintLayout>(R.id.detail)).visibility = View.GONE

                            }
                        })

                    }})
                it.isShadowCaster = false
                it.isShadowReceiver = false
                addNodeToScene(arFragment, anchor, it, isLipstick)
            }
            .exceptionally { throwable: Throwable ->
                val builder = AlertDialog.Builder(this)
                builder.setMessage(throwable.message)
                    .setTitle("Error!")
                val dialog = builder.create()
                dialog.show()
                null
            }
    }

    private fun addNodeToScene(fragment: ArFragment?, anchor: Anchor, renderable: Renderable, isLipstick: Boolean) {
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(fragment?.transformationSystem)
        node.translationController.isEnabled = false;
        node.localRotation = Quaternion.axisAngle(Vector3(1f, 0f, 0f), 270f)
        node.scaleController.minScale = 0.2f
        node.scaleController.maxScale = 0.3f
        node.localScale = Vector3(0.05f, 0.05f, 0.05f)
        val worldPos = node.worldPosition
        if(isLipstick){
            node.worldPosition = Vector3(worldPos.x + 0.13f, worldPos.y, worldPos.z + 0.1f)
        }else{
            node.worldPosition = Vector3(worldPos.x - 0.13f, worldPos.y, worldPos.z + 0.1f)
        }
        node.renderable = renderable
        node.setParent(anchorNode)
        fragment?.arSceneView?.scene?.addChild(anchorNode)
        node.select()
    }

    public override fun onPause() {
        super.onPause()
        if (mSession != null) {
            arSceneView?.pause()
            mSession?.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (mSession == null) {
            var message: String? = null
            var exception: Exception? = null
            try {
                mSession = Session(this)
            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update android"
                exception = e
            } catch (e: Exception) {
                message = "AR is not supported"
                exception = e
            }
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Exception creating session", exception)
                return
            }
            sessionConfigured = true
        }
        if (sessionConfigured) {
            configureSession()
            sessionConfigured = false
            arSceneView?.setupSession(mSession)
        }
    }

    private fun configureSession() {
        val config = Config(mSession)
        if (!setupAugmentedImageDb(config)) {
            Toast.makeText(this, "Unable to setup augmented", Toast.LENGTH_SHORT).show()
        }
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        mSession?.configure(config)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}