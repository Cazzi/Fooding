package com.example.fooding;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
/*
BrowseActivity is the one that lets the customer choose the restaurant to order from. He can choose to go to his favourites
(see FavActivity) and filter by restaurant type with a spinner
 */
public class BrowseActivity extends AppCompatActivity implements BrowseAdapter.ItemClickListener{

    FirebaseDatabase database;
    DatabaseReference myRef ;
    List<Restaurant> list = new ArrayList<>();
    RecyclerView recycle;
    TextView emptyView;
    Button fav_btn;
    BrowseAdapter adapter;
    private RecyclerView.LayoutManager rLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        recycle = (RecyclerView) findViewById(R.id.restaurant_rView);
        emptyView = (TextView) findViewById(R.id.empty_view);
        fav_btn = findViewById(R.id.fav_btn);

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        fav_btn.setOnClickListener(e -> {
            Intent i = new Intent(this, FavActivity.class);
            startActivity(i);
        });

        Spinner spinner = (Spinner) findViewById(R.id.type_et);
        String[] types = getResources().getStringArray(R.array.food_types);
        final List<String> typesList = new ArrayList<>(Arrays.asList(types));
        // Initializing an ArrayAdapter
        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                BrowseActivity.this ,R.layout.support_simple_spinner_dropdown_item, typesList){
            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    // Disable the first item from Spinner
                    // First item will be use for hint
                    return false;
                }
                else
                {
                    return true;
                }
            }
            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        spinnerArrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItemText = (String) parent.getItemAtPosition(position);
                // If user change the default selection
                // First item is disable and it is used for hint
                if(position > 0){
                    // Notify the selected item text
                    Toast.makeText
                            (getApplicationContext(), "Selected : " + selectedItemText, Toast.LENGTH_SHORT)
                            .show();

                    list.clear();

                    myRef.child("types").child(selectedItemText).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            // This method is called once with the initial value and again
                            // whenever data at this location is updated.

                            for(DataSnapshot dataSnapshot1 :dataSnapshot.getChildren()){
                                // Download the restaurants of chosen type.
                                Restaurant fire = new Restaurant();
                                fire.setUid(dataSnapshot1.getKey());
                                fire.setUri(dataSnapshot1.getKey()+"/photo.jpg");
                                myRef.child("restaurateur").child(dataSnapshot1.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        fire.setName(dataSnapshot.child("name").getValue().toString());
                                        fire.setType(dataSnapshot.child("type").getValue().toString());
                                        myRef.child("reviews").child(fire.getUid()).child("media").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                                                if(dataSnapshot2.getValue() != null) {
                                                    fire.setRating(dataSnapshot2.getValue().toString());
                                                }
                                                else {
                                                    fire.setRating("0");
                                                }

                                                list.add(fire);
                                                // Sort by rating.
                                                Collections.sort(list, Collections.reverseOrder());

                                                rLayoutManager = new LinearLayoutManager(BrowseActivity.this);
                                                recycle.setLayoutManager(rLayoutManager);
                                                adapter = new BrowseAdapter(BrowseActivity.this, list);
                                                adapter.setClickListener(BrowseActivity.this);
                                                recycle.setAdapter(adapter);

                                                // If there is no restaurant available, textview alerts the user.
                                                if (list.isEmpty()) {
                                                    recycle.setVisibility(View.GONE);
                                                    emptyView.setVisibility(View.VISIBLE);
                                                }
                                                else {
                                                    emptyView.setVisibility(View.GONE);
                                                    recycle.setVisibility(View.VISIBLE);
                                                }

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });




                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                            }





                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Failed to read value
                            Log.w("Hello", "Failed to read value.", error.toException());
                        }
                    });


                }


            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });



    }

    @Override
    public void onItemClick(View view, int position) {
        Intent i = new Intent(getApplicationContext(), MenuActivity.class);
        i.putExtra("restaurant", adapter.getItem(position));
        i.putExtra("position", position);
        startActivity(i);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        finish();
        return true;
    }
}
