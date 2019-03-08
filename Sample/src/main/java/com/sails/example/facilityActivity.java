package com.sails.example;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.ArrayList;

public class facilityActivity extends Activity {

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    Intent intent;
    int floor_num;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facility);
        final ArrayList<RecyclerData> facility = new ArrayList<>();

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new RecyclerAdapter(facility);
        mRecyclerView.setAdapter(mAdapter);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        intent = getIntent();
        floor_num = intent.getIntExtra("층이름", 0);

        facility.add(new RecyclerData("편의시설", ""));
        facility.add(new RecyclerData("연구실", ""));
        facility.add(new RecyclerData("교수연구실 및 사무실", ""));

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getApplicationContext(), mRecyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position){
                        if(position == 0){
                            new AlertDialog.Builder(facilityActivity.this)
                                    .setMessage("정수기\n자판기\n엘리베이터\n계단1\n계단2\n여자화장실\n남자화장실")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(facilityActivity.this,MainActivity.class);
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        } else if(position == 1 && floor_num == 3){
                            new AlertDialog.Builder(facilityActivity.this)
                                    .setMessage("대기과학과회의실\n기후역학실험연구실\n세미나실\n시공간데이터베이스연구실\n신경회로망 및 로봇비전 연구실\n소프트웨어 및 시스템보안 연구실\n차세대방송 및 통신연구실\n대기분석실험실\n데이터사이언스연구실")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(facilityActivity.this,MainActivity.class);
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        } else if(position == 2 && floor_num ==3){
                            new AlertDialog.Builder(facilityActivity.this)
                                    .setMessage("학과사무실\n이기준 교수연구실\n권혁철 교수연구실\n조환규 교수연구실\n우균 교수연구실\n권현주 교수연구실\n최윤호 교수연구실\n차의영 교수연구실\n김경석 교수연구실\n권준호 교수연구실\n김정구 교수연구실\n엄준식 교수연구실")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(facilityActivity.this,MainActivity.class);
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        } else if(position == 1 && floor_num == 2){
                            new AlertDialog.Builder(facilityActivity.this)
                                    .setMessage("인공지능연구실\n그래픽스응용연구실\n프로그래밍언어연구실\n객체지향시스템연구실\n이동통신연구실\n오토마타연구실\n임베디드시스템연구실\n데이터베이스 및 인터넷컴퓨팅연구실\n재료실\n네트워크컴퓨팅연구실\n모바일시스템연구실")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(facilityActivity.this,MainActivity.class);
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        } else if(position ==2 && floor_num == 2){
                            new AlertDialog.Builder(facilityActivity.this)
                                    .setMessage("김길용 교수연구실\n채흥석 교수연구실\n백윤주 교수연구실\n김종덕 교수연구실\n이현열 교수연구실\n탁성우 교수연구실")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(facilityActivity.this,MainActivity.class);
                                            startActivity(intent);
                                        }
                                    })
                                    .show();
                        }
                    }
                    @Override
                    public void onLongItemClick(View view, int position){
                    }
                })
        );
    }
}
