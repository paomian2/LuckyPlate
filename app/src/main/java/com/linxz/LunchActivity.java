package com.linxz;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.linxz.view.LuckyPlate;

public class LunchActivity extends AppCompatActivity {

    private LuckyPlate luckyPlate;
    private ImageView btnLucyController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lunch);

        luckyPlate= (LuckyPlate) findViewById(R.id.luckyPlate);
        btnLucyController= (ImageView) findViewById(R.id.btnLuckyController);

    }

    public void onClick(View v){
       if (!luckyPlate.isStart()){
           luckyPlate.luckyStart();
           btnLucyController.setImageResource(R.drawable.stop);
       }else{
           if (!luckyPlate.isShouldEnd()){
               luckyPlate.lucyEndRondom();
               btnLucyController.setImageResource(R.drawable.start);
           }
       }
    }
}
