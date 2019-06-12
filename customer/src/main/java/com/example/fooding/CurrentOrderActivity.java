package com.example.fooding;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
/*
Activity showing the current order made by the customer, letting him do a review once the order has been delivered.
 */
public class CurrentOrderActivity extends AppCompatActivity {
    private Order order;
    private TextView name_tv;
    private TextView distance_tv;
    private Button accept_btn;
    private Button review_btn;
    private Button call_btn;
    private DatabaseReference database;
    private StorageReference storage;
    private StorageReference photoref;
    private ImageView photo_iv;
    private String uid;
    private String rid;
    private int status;
    private String oid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_curr_order);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        database= FirebaseDatabase.getInstance().getReference();
        storage= FirebaseStorage.getInstance().getReference();
        uid= FirebaseAuth.getInstance().getCurrentUser().getUid();

        name_tv=findViewById(R.id.name_tv);
        distance_tv=findViewById(R.id.distance_tv);
        accept_btn=findViewById(R.id.accept_btn);
        review_btn=findViewById(R.id.review_btn);
        call_btn=findViewById(R.id.call_btn);
        photo_iv=findViewById(R.id.photo_iv);

        database.child("customer").child(uid).child("currentOrder").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                name_tv.setText(dataSnapshot.child("address").getValue().toString());
                distance_tv.setText(dataSnapshot.child("deliveryTime").getValue().toString());
                int status=Integer.parseInt(dataSnapshot.child("status").getValue().toString());
                rid=dataSnapshot.child("rid").getValue().toString();
                oid=dataSnapshot.child("oid").getValue().toString();
                switch(status){
                        case 0:
                            photo_iv.setImageResource(R.mipmap.new_order);
                            break;
                        case 1:
                            photo_iv.setImageResource(R.mipmap.cooking);
                            break;
                        case 2:
                            photo_iv.setImageResource(R.mipmap.ready);
                            break;
                        case 3:
                            photo_iv.setImageResource(R.mipmap.in_delivery);
                            break;
                        case 4:
                            photo_iv.setImageResource(R.mipmap.delivered);
                            break;
                    }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        accept_btn.setOnClickListener(e->{
            if(status!=4){
                Toast.makeText(CurrentOrderActivity.this, "Order is still not delivered!", Toast.LENGTH_SHORT).show();
                return;
            }
            final CharSequence[] choices = { "Yes", "No"};
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(CurrentOrderActivity.this);
            dialogBuilder.setTitle("Leave no review for restaurant?");
            dialogBuilder.setItems(choices, (dial, choice) -> {
                if (choices[choice].equals("Yes")) {
                    database.child("customer").child(uid).child("currentOrder").removeValue();
                    Toast.makeText(CurrentOrderActivity.this, "Delivery accepted!", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (choices[choice].equals("No")) {
                    dial.dismiss();
                }
            });
            dialogBuilder.show();
        });

        call_btn.setOnClickListener(e->{
            database.child("restaurateur").child(rid).child("telephone").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:"+dataSnapshot.getValue().toString()));
                    startActivity(intent);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        });

        review_btn.setOnClickListener(view->{
            if(status!=4){
                Toast.makeText(CurrentOrderActivity.this, "Can't review undelivered order!", Toast.LENGTH_SHORT).show();
                return;
            }

            view=getLayoutInflater().inflate(R.layout.dialog_review, null);
            AlertDialog alertDialog = new AlertDialog.Builder(CurrentOrderActivity.this).create();
            alertDialog.setTitle("Leave a review");

            final EditText text_et=view.findViewById(R.id.text_et);
            final RatingBar rate_rb=view.findViewById(R.id.rate_rb);

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "SEND", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DatabaseReference dr=database.child("reviews").child(rid).child(oid);
                    dr.child("rate").setValue(Float.toString(rate_rb.getRating()));
                    dr.child("comment").setValue(text_et.getText().toString());
                    database.child("reviews").child(rid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            float numreviews=((float)dataSnapshot.getChildrenCount())-1;
                            float tot=0;
                            for(DataSnapshot dataSnapshot1:dataSnapshot.getChildren()){
                                if(dataSnapshot1.getKey()!="media"){
                                    if(dataSnapshot1.child("rate").getValue()==null)
                                        continue;
                                    tot+=Float.parseFloat(dataSnapshot1.child("rate").getValue().toString());
                                }
                            }
                            float avg=tot/numreviews;
                            database.child("reviews").child(rid).child("media").setValue(Float.toString(avg));
                            database.child("customer").child(uid).child("currentOrder").removeValue();
                            Toast.makeText(CurrentOrderActivity.this, "Review sent!", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            });


            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.dismiss();
                }
            });

            alertDialog.setView(view);
            alertDialog.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        database.child("customer").child(uid).child("currentOrder").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                status=Integer.parseInt(dataSnapshot.child("status").getValue().toString());
                switch(status){
                    case 0:
                        photo_iv.setImageResource(R.mipmap.new_order);
                        break;
                    case 1:
                        photo_iv.setImageResource(R.mipmap.cooking);
                        break;
                    case 2:
                        photo_iv.setImageResource(R.mipmap.ready);
                        break;
                    case 3:
                        photo_iv.setImageResource(R.mipmap.in_delivery);
                        break;
                    case 4:
                        photo_iv.setImageResource(R.mipmap.delivered);
                        break;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }
}
