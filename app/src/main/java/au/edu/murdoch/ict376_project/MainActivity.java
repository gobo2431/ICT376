//-----------------------------------------------------------------------------------------------------------------------

package au.edu.murdoch.ict376_project;

//-----------------------------------------------------------------------------------------------------------------------

//Author: Austin Dennis (32767615)
//Date: 17/09/2020
//Brief: TODO

//-----------------------------------------------------------------------------------------------------------------------

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//-----------------------------------------------------------------------------------------------------------------------

public class MainActivity extends AppCompatActivity implements CameraXConfig.Provider
{

    //-----------------------------------------------------------------------------------------------------------------------

    private String[] permissions =
    {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };

    private static final int PERMISSION_CODE = 101;
    private static final String TAG = "CameraX";

    //Camera shit
    private ImageCapture imageCapture;
    private ProcessCameraProvider cameraProvider;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private int lensFacing;

    //Image captured
    private ByteArrayOutputStream latestImage;

    //-----------------------------------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        //Setting btn on click listeners
        configCaptureBtn();
        configGalleryBtn();
        configRotateBtn();

        if (!hasPermissions(permissions))
        {
            requestPermissions(permissions, PERMISSION_CODE);
        }
        else
        {
            startCamera();
        }

    }

    //-----------------------------------------------------------------------------------------------------------------------

    private boolean hasPermissions(String[] permissions)
    {
        for (int i = 0; i < permissions.length; i++)
        {
            if (ActivityCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
        }

        return true;
    }

    //------------------------------------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case PERMISSION_CODE:
            {
                if (grantResults.length > 0)
                {
                    for (int i = 0; i < permissions.length; i++)
                    {
                        if(grantResults[0] == PackageManager.PERMISSION_DENIED)
                        {
                            Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
                            finish();
                        }

                    }

                    startCamera();
                }
            }
        }
    }


    //-----------------------------------------------------------------------------------------------------------------------

    public void configCaptureBtn()
    {
        ImageButton captureBtn = (ImageButton) findViewById(R.id.captureBtn);

        captureBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                captureImage();
            }
        });
    }

    //-----------------------------------------------------------------------------------------------------------------------

    private void configGalleryBtn()
    {
        ImageButton galleryBtn = (ImageButton) findViewById(R.id.galleryBtn);

        galleryBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //TODO: change to open image gallery we make
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("file/*");
                startActivity(intent);
            }
        });
    }

    //-----------------------------------------------------------------------------------------------------------------------

    public void configRotateBtn()
    {
        ImageButton switchBtn = (ImageButton)findViewById(R.id.switchBtn);

        switchBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(CameraSelector.LENS_FACING_FRONT == lensFacing)
                {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                }
                else
                {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                }

               bindCamera();
            }
        });
    }

    //-----------------------------------------------------------------------------------------------------------------------
    //Setting up CameraX
    //TODO: resolution + rotation

    @Override
    public CameraXConfig getCameraXConfig()
    {
        return Camera2Config.defaultConfig();
    }

    //-----------------------------------------------

    private boolean setLensFacing()
    {
        try
        {
            if(hasBackCamera())
            {
                lensFacing = CameraSelector.LENS_FACING_BACK;
            }
        }
        catch (CameraInfoUnavailableException e)
        {
            try
            {
                if(hasFrontCamera())
                {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                }
            }
            catch (CameraInfoUnavailableException ex)
            {
                ex.printStackTrace();
                return false;
            }
        }

        return true;
    }

    //-----------------------------------------------

    private void startCamera()
    {
        Log.d(TAG, "startCamera()");

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    cameraProvider = cameraProviderFuture.get();

                    if(setLensFacing())
                    {
                        bindCamera();
                    }
                    else
                    {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                "No camera available",
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                }
                catch (ExecutionException | InterruptedException e)
                {
                    // No errors
                }
            }

        }, ContextCompat.getMainExecutor(this));

    }

    //-----------------------------------------------

    private void bindCamera()
    {
        Log.d(TAG, "bindPreview()");

        PreviewView previewView = (PreviewView)findViewById(R.id.previewView);

        Preview preview = new Preview.Builder()
                .build();

        System.out.println("Hello");

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        imageCapture = new ImageCapture.Builder()
                .build();

        preview.setSurfaceProvider(previewView.createSurfaceProvider());

        cameraProvider.unbindAll();

        try
        {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        }
        catch (Exception e)
        {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    //-----------------------------------------------------------------------------------------------------------------------
    //Capture Image stuff
    //TODO: flip image when captured with front camera + rotation stuff

    private void captureImage()
    {
        latestImage = new ByteArrayOutputStream();

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture
                .OutputFileOptions
                .Builder(latestImage)
                .build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback()
        {
            @Override
            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults)
            {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Imaged Captured",
                        Toast.LENGTH_SHORT);
                toast.show();
            }

            @Override
            public void onError(ImageCaptureException exception)
            {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Error",
                        Toast.LENGTH_SHORT);
                toast.show();

                Log.e("Capture error", exception.getMessage());
            }
        });

    }

    //-----------------------------------------------------------------------------------------------------------------------

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        // Enable or disable front and back camera switching
        updateCameraSwitchButton();
    }

    private void updateCameraSwitchButton()
    {
        ImageButton switchBtn = (ImageButton)findViewById(R.id.switchBtn);
        try
        {
            switchBtn.setEnabled(hasBackCamera() && hasBackCamera());
        }
        catch (CameraInfoUnavailableException e)
        {
            switchBtn.setEnabled(false);
        }
    }

    private boolean hasBackCamera() throws CameraInfoUnavailableException
    {
        if(cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
        {
            //No exception thrown
        }

        return true;

    }

    private boolean hasFrontCamera() throws CameraInfoUnavailableException
    {
        if(cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA))
        {
            //No exception thrown
        }

        return true;
    }

}

//-----------------------------------------------------------------------------------------------------------------------

