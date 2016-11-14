package com.awibi.listfilm.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.awibi.listfilm.Model.Movie;
import com.awibi.listfilm.Model.Result;
import com.awibi.listfilm.R;
import com.awibi.listfilm.StaticValues;
import com.awibi.listfilm.adapter.EndlessScrollListener;
import com.awibi.listfilm.adapter.LVMovieAdapter;
import com.awibi.listfilm.api.ApiRequest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    Retrofit retrofit;
    ListView lv_movie;
    ProgressBar progressBar;

    int itemCountsPerPage=20;
    LinkedList<Result> resultsLV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv_movie=(ListView)findViewById(R.id.lv_movies);
        progressBar=(ProgressBar)findViewById(R.id.pb_loading);
        resultsLV=new LinkedList<>();

        retrofit=new Retrofit.Builder()
                .baseUrl(StaticValues.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        if(isNetworkAvailable(this)==true){
            LVgetPopulerMovie(1);
        }
        ShowNextContent();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    public void ShowNextContent(){
        if (isNetworkAvailable(this)==true){
            lv_movie.setOnScrollListener(new EndlessScrollListener() {
                @Override
                public boolean onLoadMore(int page, int totalItemsCount) {
                    LVgetPopulerMovie(page);
                    return true;
                }
            });


        }else{
            AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("No Internet Connection");
            builder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ShowNextContent();
                }
            });
            AlertDialog dialog=builder.create();
            dialog.show();
        }
    }

    public boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }


    public void LVgetPopulerMovie(int page){

        final ProgressDialog progressDialog=new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("loading");
        progressDialog.show();

        ApiRequest apiRequest=retrofit.create(ApiRequest.class);

        Call<Movie> movieCall=apiRequest.PopulerMovieList(StaticValues.API_KEY,"popularity.desc",page);

        //eksekusi api
        movieCall.enqueue(new Callback<Movie>() {
            @Override
            public void onResponse(Call<Movie> call, Response<Movie> response) {
                if(response.isSuccessful()){

                    Log.d("retrofit","berhasil");
                    progressBar.setVisibility(View.GONE);
                    Movie movie=response.body();

                    for(int i=0;i<movie.getResults().size();i++){
                        Result result = movie.getResults().get(i);
                        resultsLV.addLast(result);
                        Log.d("aaaa1",""+result.getTitle());
                        Log.d("aaaa2",""+result.getPosterPath());
                        Log.d("aaaa3",""+result.getReleaseDate());
                    }

                    Log.d("retrofit hasil",""+resultsLV.size());
                    //populate to list view
                    LVMovieAdapter adapter=new LVMovieAdapter(resultsLV,MainActivity.this);
                    lv_movie.setAdapter(adapter);
                    adapter.notifyDataSetChanged();

                    lv_movie.setSelection(resultsLV.size()-(itemCountsPerPage)-1);
                    //
                    lv_movie.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Intent intent=new Intent(MainActivity.this,DetailActivity.class);

                            intent.putExtra(StaticValues.TITLE_KEY,resultsLV.get(position).getTitle());
                            intent.putExtra(StaticValues.POSTER_KEY,resultsLV.get(position).getPosterPath().toString());
                            intent.putExtra(StaticValues.DESCRIPTION_KEY,resultsLV.get(position).getOverview());
                            intent.putExtra(StaticValues.RATING_KEY,resultsLV.get(position).getVoteAverage());
                            intent.putExtra(StaticValues.RELEASE_KEY,resultsLV.get(position).getReleaseDate());

                            startActivity(intent);
                        }
                    });

                    progressDialog.dismiss();

                }
            }

            @Override
            public void onFailure(Call<Movie> call, Throwable t) {
                Log.d("retrofit","gagal");
            }
        });

    }
}
