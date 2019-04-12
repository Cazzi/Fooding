package com.example.ristoratore;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.ristoratore.menu.Dish;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class AddDishActivity extends AppCompatActivity {

    private static final int GALLERY_REQCODE = 31;
    private static final int CAM_REQCODE = 32;
    private static final int STORAGE_PERM_CODE = 33;

    public static final String DISH_NAME_PREFS = "dish_name_prefs";
    public static final String DISH_DESC_PREFS = "dish_desc_prefs";
    public static final String DISH_PRICE_PREFS = "dish_price_prefs";
    public static final String DISH_QTY_PREFS = "dish_qty_prefs";
    public static final String DISH_PHOTO_PREFS = "dish_photo_prefs";

    private Dish dish;

    private ImageView photo;
    private EditText name_et;
    private EditText desc_et;
    private EditText price_et;
    private EditText qty_et;
    private Uri selectedPhoto;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dish_descriptor);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = preferences.edit();

        photo = findViewById(R.id.dish_photo_iv);
        name_et = findViewById(R.id.dish_name_et);
        desc_et = findViewById(R.id.dish_desc_et);
        price_et = findViewById(R.id.dish_price_et);
        qty_et = findViewById(R.id.dish_qty_et);
        Button save_btn = findViewById(R.id.save_dish_btn);

        if(preferences.contains(DISH_PHOTO_PREFS)) {
            photo.setImageURI(Uri.parse(preferences.getString(DISH_PHOTO_PREFS, "")));
            if (photo.getDrawable() == null)
                photo.setImageResource(R.drawable.ic_launcher_foreground);
        }
        if(preferences.contains(DISH_NAME_PREFS))
            name_et.setText(preferences.getString(DISH_NAME_PREFS, ""));
        if(preferences.contains(DISH_DESC_PREFS))
            desc_et.setText(preferences.getString(DISH_DESC_PREFS, ""));
        if(preferences.contains(DISH_PRICE_PREFS))
            price_et.setText(String.valueOf(preferences.getFloat(DISH_PRICE_PREFS, 0)));
        if(preferences.contains(DISH_QTY_PREFS))
            qty_et.setText(String.valueOf(preferences.getInt(DISH_QTY_PREFS, 0)));

        if (savedInstanceState != null){
            if(savedInstanceState.containsKey("uri_photo")) {
                selectedPhoto = savedInstanceState.getParcelable("uri");
                photo.setImageURI(selectedPhoto);
            }
        }

        photo.setOnClickListener(e -> {
            if(isStoragePermissionGranted()) { /* TO-DO : check if before I had permissions !!!!!!!!!!!!!!!!!!!!! */
                selectImage();
            }
        });

        save_btn.setOnClickListener(v -> {
            String name = name_et.getText().toString();
            String description = desc_et.getText().toString();
            ImageView photo = this.photo;
            float price = Float.parseFloat(price_et.getText().toString());
            int qty = Integer.parseInt(qty_et.getText().toString());

            dish = new Dish(name, description, photo, price, qty);
            Log.d("MAD-dish", "Created dish: " + dish.getName() + dish.getDescription() +
                                                dish.getPhoto().toString() + dish.getPrice() + dish.getAvailable_qty());

            if(!(name_et.getText().toString().equals(preferences.getString(DISH_NAME_PREFS, "")))) {
                editor.putString(DISH_NAME_PREFS, name_et.getText().toString());
                editor.apply();
            }
            if(!(desc_et.getText().toString().equals(preferences.getString(DISH_DESC_PREFS, "")))) {
                editor.putString(DISH_DESC_PREFS, desc_et.getText().toString());
                editor.apply();
            }
            if(!(Float.parseFloat(price_et.getText().toString()) == preferences.getFloat(DISH_PRICE_PREFS, 0))) {
                editor.putFloat(DISH_PRICE_PREFS, Float.parseFloat(price_et.getText().toString()));
                editor.apply();
            }
            if(!(Integer.parseInt(qty_et.getText().toString()) == preferences.getInt(DISH_QTY_PREFS, 0))) {
                editor.putInt(DISH_QTY_PREFS, Integer.parseInt(qty_et.getText().toString()));
                editor.apply();
            }
            if(selectedPhoto != null && !(selectedPhoto.toString().equals(preferences.getString(DISH_PHOTO_PREFS, "")))) {
                editor.putString(DISH_PHOTO_PREFS, selectedPhoto.toString());
                editor.apply();
            }
            finish();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(selectedPhoto != null && !(selectedPhoto.toString().equals(preferences.getString(DISH_PHOTO_PREFS, ""))))
            outState.putParcelable("uri_photo", selectedPhoto);
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermission())
                return true;
            else
                requestStoragePermission();
        }
        else {
            return true;
        }
        return false;
    }

    private void selectImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Library", "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(AddDishActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, (dialog, item) -> {
            if (items[item].equals("Take Photo")) {
                cameraIntent();
            } else if (items[item].equals("Choose from Library")) {
                galleryIntent();
            } else if (items[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    @SuppressLint("IntentReset")
    private void galleryIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, GALLERY_REQCODE);
    }

    private void cameraIntent() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        selectedPhoto = FileProvider.getUriForFile(AddDishActivity.this, BuildConfig.APPLICATION_ID + ".provider", Objects.requireNonNull(getOutputMediaFile()));
        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, selectedPhoto);
        takePicture.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(takePicture, CAM_REQCODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {

        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case CAM_REQCODE:
                if (resultCode == RESULT_OK) {
                    photo.setImageURI(selectedPhoto);
                }
                break;
            case GALLERY_REQCODE:
                if (imageReturnedIntent != null && resultCode == RESULT_OK) {
                    selectedPhoto = imageReturnedIntent.getData();
                    photo.setImageURI(selectedPhoto);
                }
                break;
        }
    }

    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MAD-photo", "Oops! Failed create directory");
                return null;
            }
        }

        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +"IMG_"+ timeStamp + ".jpg");
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(AddDishActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed to store images")
                    .setPositiveButton("Ok", (dialog, which) -> ActivityCompat.requestPermissions(AddDishActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERM_CODE))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERM_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case STORAGE_PERM_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}