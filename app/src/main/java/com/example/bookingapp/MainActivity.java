package com.example.bookingapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import com.example.bookingapp.R;

import java.math.BigDecimal;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(com.example.bookingapp.R.layout.activity_main);

        //Get the elements
        EditText input1 = findViewById(R.id.input1);
        EditText input2 = findViewById(R.id.input2);
        TextView resultField = findViewById(R.id.resultField);
        Button calculateBtn = findViewById(R.id.calculateBtn);
        EditText databaseinput = findViewById(R.id.databaseinput);
        Button databasebutton = findViewById(R.id.databasebutton);

        //PART WITH THE DB


        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("entries");

        databasebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String value = databaseinput.getText().toString().trim();

                if (value.isEmpty()) {
                    databaseinput.setError("Field can't be empty");
                    return;
                }

                // Creates a unique key under "entries" and sets the value
                dbRef.push().setValue(value)
                        .addOnSuccessListener(unused -> {
                            databaseinput.setText(""); // Clear field on success
                        })
                        .addOnFailureListener(e -> {
                            databaseinput.setError("Failed: " + e.getMessage());
                        });
            }
        });




        //CODE FROM THE LAB TEMPLATE, REMOVE THE STUFF BELOW


        //set trigger for the button onClick
        calculateBtn.setOnClickListener(new View.OnClickListener(){

            //User clicks button-> calculate sum of two inputs
            @Override
            public void onClick(View view){
                String num1 = input1.getText().toString();
                String num2 = input2.getText().toString();

                if (num1.isEmpty() || num2.isEmpty()){ //Input validation- empty input
                    resultField.setText("Input can't be null"); //gets caught by the Catch, but still keeping it in case
                }

                try{
                    //set to BigInteger (BigDecimal is best) instead of int due to extremely large numbers that might change the number
                    BigDecimal n1 = new BigDecimal(num1);
                    BigDecimal n2 = new BigDecimal(num2);
                    BigDecimal sum = n1.add(n2);

                    //display
                    resultField.setText("= "+sum);
                }
                catch(NumberFormatException e){
                    resultField.setText("Wrong input: can't be null or non-numeric");
                }
            }

        });




    }






}