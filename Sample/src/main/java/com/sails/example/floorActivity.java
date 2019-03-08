package com.sails.example;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class floorActivity extends Activity {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floor);

        final ArrayList<RecyclerData> arrayListofFloor = new ArrayList<>();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new RecyclerAdapter(arrayListofFloor);
        mRecyclerView.setAdapter(mAdapter);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        arrayListofFloor.add(new RecyclerData("1층",""));
        arrayListofFloor.add(new RecyclerData("2층",""));
        arrayListofFloor.add(new RecyclerData("3층",""));
        arrayListofFloor.add(new RecyclerData("4층",""));
        arrayListofFloor.add(new RecyclerData("5층",""));


        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position){
                        Intent intent=new Intent(floorActivity.this,facilityActivity.class);
                        intent.putExtra("층이름", position);
                        startActivity(intent);
                    }
                    @Override
                    public void onLongItemClick(View view, int position){
                    }
                })
        );
    }
}
