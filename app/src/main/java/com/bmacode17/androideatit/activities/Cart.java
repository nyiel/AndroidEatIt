package com.bmacode17.androideatit.activities;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bmacode17.androideatit.R;
import com.bmacode17.androideatit.adapters.CartAdapter;
import com.bmacode17.androideatit.common.Common;
import com.bmacode17.androideatit.databases.Database;
import com.bmacode17.androideatit.models.Food;
import com.bmacode17.androideatit.models.MyNotification;
import com.bmacode17.androideatit.models.MyResponse;
import com.bmacode17.androideatit.models.Order;
import com.bmacode17.androideatit.models.Request;
import com.bmacode17.androideatit.models.Sender;
import com.bmacode17.androideatit.models.Token;
import com.bmacode17.androideatit.remotes.APIService;
import com.bmacode17.androideatit.viewHolders.FoodViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.hoang8f.widget.FButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Cart extends AppCompatActivity {

    private static final String TAG = "Basel";
    FirebaseDatabase database;
    DatabaseReference table_request;
    RecyclerView recyclerView_listCart;
    RecyclerView.LayoutManager layoutManager;
    TextView textView_totalPrice;
    EditText editText_address, editText_notes;
    FButton button_placeOrder;
    List<Order> carts = new ArrayList<>();
    CartAdapter cartAdapter;
    AlertDialog addressDialog;

    APIService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // Init service
        mService = Common.getFCMService();

        button_placeOrder = (FButton) findViewById(R.id.button_placeOrder);
        textView_totalPrice = (TextView) findViewById(R.id.textView_totalPrice);

        database = FirebaseDatabase.getInstance();
        table_request = database.getReference("request");

        recyclerView_listCart = (RecyclerView) findViewById(R.id.recyclerView_listCart);
        recyclerView_listCart.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView_listCart.setLayoutManager(layoutManager);

        loadCartList();

        button_placeOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (carts.size() > 0)
                    openAddressDialog();
                else
                    Toast.makeText(Cart.this, "Your cart is empty ! ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openAddressDialog() {

        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.addressdialog, null);
        myAlertDialog.setView(dialogView);
        myAlertDialog.setCancelable(true);
        myAlertDialog.setTitle("One more step !");
        editText_address = (EditText) dialogView.findViewById(R.id.editText_address);
        editText_notes = (EditText) dialogView.findViewById(R.id.editText_notes);
        myAlertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(Cart.this, "Order is canceled", Toast.LENGTH_LONG).show();
            }
        });

        myAlertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Request request = new Request(
                        Common.currentUser.getPhone(),
                        Common.currentUser.getName(),
                        editText_address.getText().toString(),
                        textView_totalPrice.getText().toString(),
                        carts,
                        editText_notes.getText().toString());
                // Submit to firebase
                // currentTimeMillis is considered as a key
                String orderNumber = String.valueOf(System.currentTimeMillis());
                table_request.child(orderNumber).setValue(request);
                // Delete carts
                new Database(getBaseContext()).cleanCarts();
                sendOrderNotification(orderNumber);
            }
        });

        addressDialog = myAlertDialog.create();
        addressDialog.show();
    }

    private void sendOrderNotification(final String orderNumber) {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference table_token = db.getReference("token");
        Query data = table_token.orderByChild("serverToken").equalTo(true);  // Get all nodes which its isServerToken is true
        data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot item : dataSnapshot.getChildren()) {

                    Token serverToken = item.getValue(Token.class);

                    // Create Raw payload to send
                    MyNotification notification = new MyNotification("Basel", "New order: " + orderNumber);
                    Sender content = new Sender(serverToken.getToken(), notification);
                    mService.sendNotification(content)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {

                                    if(response.code() == 200){

                                        if (response.body().success == 1) {
                                            Toast.makeText(Cart.this, "Thank you , order is placed ", Toast.LENGTH_LONG).show();
                                            finish();
                                        }else
                                            Toast.makeText(Cart.this, "Failed ", Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {
                                    Log.d(TAG, "onFailure: Error: " + t.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void loadCartList() {

        carts = new Database(this).getCarts();
        cartAdapter = new CartAdapter(carts, this);
        cartAdapter.notifyDataSetChanged();
        recyclerView_listCart.setAdapter(cartAdapter);

        int totalPrice = 0;
        for (Order i : carts)
            totalPrice += Integer.parseInt(i.getPrice()) * Integer.parseInt(i.getQuantity());
        Locale locale = new Locale("en", "US");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
        textView_totalPrice.setText(fmt.format(totalPrice));

    }

    public boolean onContextItemSelected(MenuItem item) {

        if (item.getTitle().equals(Common.DELETE))
            deleteCart(item.getOrder());

        return super.onContextItemSelected(item);
    }

    private void deleteCart(int position) {

        // we'll remove item from list<order> by position
        carts.remove(position);
        // then , we delete all old data from SQLite
        new Database(this).cleanCarts();
        // finally , we'll update the SQLite using the new updated carts
        for (Order item : carts)
            new Database(this).addToCarts(item);
        loadCartList();
    }
}
