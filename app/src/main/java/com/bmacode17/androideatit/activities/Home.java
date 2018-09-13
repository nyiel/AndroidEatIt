package com.bmacode17.androideatit.activities;

import com.bmacode17.androideatit.R;
import com.bmacode17.androideatit.common.Common;
import com.bmacode17.androideatit.databases.Database;
import com.bmacode17.androideatit.interfaces.ItemClickListener;
import com.bmacode17.androideatit.models.Category;
import com.bmacode17.androideatit.models.Request;
import com.bmacode17.androideatit.models.Token;
import com.bmacode17.androideatit.viewHolders.MenuViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Picasso;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "Basel";
    FirebaseDatabase database;
    DatabaseReference table_category;
    TextView textView_fullName;
    RecyclerView recyclerView_menu;
    RecyclerView.LayoutManager layoutManager;
    FirebaseRecyclerAdapter<Category, MenuViewHolder> adapter;
    AlertDialog changePasswordDialog;
    EditText editText_oldPassword, editText_newPassword, editText_repeatNewPassword;
    SwipeRefreshLayout swipeRefreshLayout_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Menu");
        setSupportActionBar(toolbar);

        Paper.init(this);

        // Init firebase
        database = FirebaseDatabase.getInstance();
        table_category = database.getReference("category");

        swipeRefreshLayout_home = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout_home);
        swipeRefreshLayout_home.setColorSchemeResources(R.color.colorPrimary
                , android.R.color.holo_green_dark
                , android.R.color.holo_orange_dark
                , android.R.color.holo_blue_dark);
        swipeRefreshLayout_home.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                if(Common.isConnectedToInternet(getBaseContext()))
                    loadMenu();
                else{
                    Toast.makeText(Home.this, "Check your Internet connection !", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        // Default , load for the first time

        swipeRefreshLayout_home.post(new Runnable() {
            @Override
            public void run() {

                if(Common.isConnectedToInternet(getBaseContext()))
                    loadMenu();
                else{
                    Toast.makeText(Home.this, "Check your Internet connection !", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cartIntent = new Intent(Home.this,Cart.class);
                startActivity(cartIntent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Set Name for user
        View headerView = navigationView.getHeaderView(0);
        textView_fullName = (TextView) headerView.findViewById(R.id.textView_fullName);
        textView_fullName.setText(Common.currentUser.getName());

        // Load menu
        // Use firebase UI to bind data from Firebase to Recycler view
        recyclerView_menu = (RecyclerView) findViewById(R.id.recyclerView_menu);
        recyclerView_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView_menu.setLayoutManager(layoutManager);

        updateToken(FirebaseInstanceId.getInstance().getToken());
    }

    private void updateToken(String token) {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference table_token = db.getReference("token");
        Token data = new Token(token , false);  // false because this token is sent from the client
        table_token.child(Common.currentUser.getPhone()).setValue(data);
    }

    private void loadMenu() {

        adapter = new FirebaseRecyclerAdapter<Category, MenuViewHolder>(Category.class, R.layout.menu_item_cardview,
                MenuViewHolder.class, table_category) {
            @Override
            protected void populateViewHolder(MenuViewHolder viewHolder, Category model, int position) {

                viewHolder.textView_menuName.setText(model.getName());
                Picasso.with(getBaseContext()).load(model.getImage()).into(viewHolder.imageView_menuImage);
                final Category clickedItem = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {

                        // Get categoryID and send it to new activity
                        Intent foodListIntent = new Intent(Home.this,FoodList.class);
                        // CategoryId is a key , so we just get the key of the clicked item
                        foodListIntent.putExtra("categoryId" , adapter.getRef(position).getKey());
                        startActivity(foodListIntent);
                    }
                });
            }
        };

        recyclerView_menu.setAdapter(adapter);
        swipeRefreshLayout_home.setRefreshing(false);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.refresh)
            loadMenu();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_menu) {

        } else if (id == R.id.nav_cart) {

            Intent cartIntent = new Intent(Home.this , Cart.class);
            startActivity(cartIntent);

        } else if (id == R.id.nav_orders) {

            Intent orderStatusIntent = new Intent(Home.this , OrderStatus.class);
            startActivity(orderStatusIntent);

        } else if (id == R.id.nav_logout) {

            Paper.book().destroy();
            Intent signInIntent = new Intent(Home.this , SignIn.class);
            signInIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(signInIntent);
        }

        else if (id == R.id.nav_changePassword) {
            showChangePasswordDialog();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showChangePasswordDialog() {

        AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.change_password_cardview, null);
        myAlertDialog.setView(dialogView);
        myAlertDialog.setCancelable(true);
        myAlertDialog.setTitle("Change Password");
        myAlertDialog.setMessage("Please fill full information");
        editText_oldPassword = (EditText) dialogView.findViewById(R.id.editText_oldPassword);
        editText_newPassword = (EditText) dialogView.findViewById(R.id.editText_newPassword);
        editText_repeatNewPassword = (EditText) dialogView.findViewById(R.id.editText_repeatNewPassword);
        myAlertDialog.setIcon(R.drawable.ic_security_black_24dp);

        myAlertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                Toast.makeText(Home.this, "Change password is canceled", Toast.LENGTH_LONG).show();
            }
        });

        myAlertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // For use SpotsDialog  , we'll use AlertDialog from android.app not from v7
                final android.app.AlertDialog waitingDialog = new SpotsDialog(Home.this);
                waitingDialog.show();

                if(editText_oldPassword.getText().toString().equals(Common.currentUser.getPassword())){

                    if(editText_newPassword.getText().toString().equals(editText_repeatNewPassword.getText().toString())){

                        Map<String,Object> updatePassword = new HashMap<>();
                        updatePassword.put("password",editText_newPassword.getText().toString());

                        DatabaseReference table_user = FirebaseDatabase.getInstance().getReference("user");
                        table_user.child(Common.currentUser.getPhone())
                                .updateChildren(updatePassword)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        waitingDialog.dismiss();
                                        Toast.makeText(Home.this, "Password is updated ", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        waitingDialog.dismiss();
                                        Toast.makeText(Home.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }else{
                        waitingDialog.dismiss();
                        Toast.makeText(Home.this, "New password doesn't match !", Toast.LENGTH_SHORT).show();
                    }

                }else{
                    waitingDialog.dismiss();
                    Toast.makeText(Home.this, "Wrong old password !", Toast.LENGTH_SHORT).show();
                }
            }
        });

        changePasswordDialog = myAlertDialog.create();
        changePasswordDialog.show();
    }
}
