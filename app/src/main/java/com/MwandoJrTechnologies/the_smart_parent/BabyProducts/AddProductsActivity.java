package com.MwandoJrTechnologies.the_smart_parent.BabyProducts;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.MwandoJrTechnologies.the_smart_parent.R;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AddProductsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    final static int galleryPick = 1;
    private Spinner productCategoryDropDown;
    private EditText editTextProductName;
    private EditText editTextProductDescription;
    private EditText editTextProductManufactureCompany;
    private ImageView productImage;
    private Button buttonUploadProduct;
    private DatabaseReference productsDatabaseReference;
    private StorageReference productImageStorageReference;
    private ProgressDialog progressDialog;
    private String downloadImageUrl;

    private String productRandomName;
    private String saveCurrentDate;
    private String saveCurrentTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_products);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);  //for the back button
        getSupportActionBar().setTitle("Add Product");


        //specify path in database
        productsDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Products");

        //specify path in fireBase storage
        productImageStorageReference = FirebaseStorage.getInstance().getReference().child("ProductImages");

        productCategoryDropDown = findViewById(R.id.product_category_spinner);
        productImage = findViewById(R.id.product_image_view);
        editTextProductName = findViewById(R.id.edit_text_product_name);
        editTextProductDescription = findViewById(R.id.edit_text_product_description);
        editTextProductManufactureCompany = findViewById(R.id.edit_text_product_manufacturer_company);
        buttonUploadProduct = findViewById(R.id.button_upload_product);

        progressDialog = new ProgressDialog(this);

        buttonUploadProduct.setOnClickListener(v ->

    {
        //show progress dialog
        progressDialog.setTitle("Product Upload");
        progressDialog.setMessage("Uploading product, Please wait...");
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.show();

        saveProductInformation();

    });

        productImage.setOnClickListener(v ->

    {
        //opening gallery to choose image
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, galleryPick);
    });

        // for category dropdown
        productCategoryDropDown.setOnItemSelectedListener(this);

        //dropdown elements
        List<String> categories = new ArrayList<String>();
        categories.add("Food");
        categories.add("Soap");
        categories.add("Diapers");
        categories.add("Baby Oil");

        //create array adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        // attaching data adapter to spinner
        productCategoryDropDown.setAdapter(dataAdapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //on selecting spinner item
        String item = parent.getItemAtPosition(position).toString();

        SaveDateAndTimeToFireBaseStorage();
        productsDatabaseReference.child(productRandomName).child("category").setValue(item);

        productsDatabaseReference.child(productRandomName).child("category").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("category")){

                    productsDatabaseReference.child(productRandomName).child("category").setValue(item);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //ignore
    }

    private void SaveDateAndTimeToFireBaseStorage() {
        //setting current date and time to generate random keys for the users images posted
        //setting current date
        Calendar callForDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
        saveCurrentDate = currentDate.format(callForDate.getTime());

        //setting current date
        Calendar callForTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(callForTime.getTime());

        productRandomName = saveCurrentDate + saveCurrentTime;

    }

    //method for picking the chosen image from my gallery
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == galleryPick && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            //adding crop image functionality using arthurHub library on github
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                //show progress dialog
                progressDialog.setTitle("Product Image");
                progressDialog.setMessage("Updating product image, Please wait...");
                progressDialog.setCanceledOnTouchOutside(true);
                progressDialog.show();

                Uri resultUri = result.getUri();
                productImage.setImageURI(resultUri);

                //creating a filepath for pushing cropped image to fireBase storage by unique user id
                final StorageReference filePath = productImageStorageReference.child(resultUri.getLastPathSegment() + editTextProductName + ".jpg");

                //now store in fireBase storage
                final UploadTask uploadTask = filePath.putFile(resultUri);

                uploadTask.addOnSuccessListener(taskSnapshot -> {

                    Task<Uri> UriTask = uploadTask.continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        //get the url...initialize downloadImageUrl at the most to ie....String downloadImageUrl
                        downloadImageUrl = filePath.getDownloadUrl().toString();
                        return filePath.getDownloadUrl();


                    }).addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            //get the link
                            downloadImageUrl = task.getResult().toString();

                            addLinkToFireBaseDatabase();

                            Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Good", Snackbar.LENGTH_SHORT);
                            snackbar.show();
                            progressDialog.dismiss();
                        }
                    });
                });
            }
        }
    }

    private void addLinkToFireBaseDatabase() {
        productsDatabaseReference.child(productRandomName).child("productImage").setValue(downloadImageUrl).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                progressDialog.dismiss();
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Product image uploaded successfully uploaded...", Snackbar.LENGTH_SHORT);
                snackbar.show();
            } else {
                progressDialog.dismiss();
                String message = task.getException().getMessage();
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Error occurred  " + message, Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        });
    }


    //Validation where both details must be filled and saved to fireBase database
    private void saveProductInformation() {
        final String productName = editTextProductName.getText().toString().trim();
        String productDescription = editTextProductDescription.getText().toString().trim();
        String productManufactureCompany = editTextProductManufactureCompany.getText().toString().trim();

        if (productName.isEmpty()) {
            editTextProductName.setError("You add a product name");
            editTextProductName.requestFocus();
            progressDialog.dismiss();
        }
        if (productDescription.isEmpty()) {
            editTextProductDescription.setError("Describe product");
            editTextProductDescription.requestFocus();
            progressDialog.dismiss();
        }
        if (productManufactureCompany.isEmpty()) {
            editTextProductManufactureCompany.setError("Which company? ");
            editTextProductManufactureCompany.requestFocus();
            progressDialog.dismiss();
        } else {

            progressDialog.setTitle("Uploading Details...");
            progressDialog.setMessage("Saving your information, Please wait...");
            progressDialog.show();
            progressDialog.setCanceledOnTouchOutside(true);


            final HashMap productMap = new HashMap();
            productMap.put("productName", productName);
            productMap.put("productDescription", productDescription);
            productMap.put("productManufactureCompany", productManufactureCompany);
            productMap.put("ratings", "ratings");
            productsDatabaseReference.child(productRandomName).updateChildren(productMap).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Product upload successful", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                    progressDialog.dismiss();

                } else {
                    String message = task.getException().getMessage();
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "An error occurred,please try again " + message, Snackbar.LENGTH_LONG);
                    snackbar.show();
                    progressDialog.dismiss();
                }
            });

            //start view product activity
            finish();
            SendUserToViewProductsActivity();
        }
    }


    //activate back button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            SendUserToViewProductsActivity();
        }
        return super.onOptionsItemSelected(item);
    }


    //open view products activity
    private void SendUserToViewProductsActivity() {
        Intent viewProductsActivityIntent = new Intent(AddProductsActivity.this, ViewProductsActivity.class);
        finish();
        startActivity(viewProductsActivityIntent);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}