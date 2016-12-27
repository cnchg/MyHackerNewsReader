package com.tricloudcommunications.ce.myhackernewsreader;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    Map<Integer, String> articleURLs = new HashMap<Integer, String>();
    Map<Integer, String> articleTitles = new HashMap<Integer, String>();
    Map<Integer, String> articleTimes = new HashMap<Integer, String>();
    ArrayList<Integer> articleIDs = new ArrayList<Integer>();

    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleID INTEGER, url VARCHAR, title VARCHAR, content VARCHAR, articleTime VARCHAR)");

        DownloadTask task = new DownloadTask();
        try {

            String result = task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();
            //Log.i("Result", result);

            JSONArray jsonArray = new JSONArray(result);

            /** Use this for loop funtion if you wanted to get all 500 articles ID's
               for(int i = 0; i < 20; i++){
                    Log.i("Article ID", jsonArray.getString(i));
                }
             **/

            articlesDB.execSQL("DELETE FROM articles");

            //In this case we only want to get 20 article ID's
            for(int i = 0; i < 20; i++){

                String articleID = jsonArray.getString(i);

                //Log.i("Article ID", jsonArray.getString(i));

                DownloadTask getArticles = new DownloadTask();

                String articleInfo = getArticles.execute("https://hacker-news.firebaseio.com/v0/item/"+articleID+".json?print=pretty").get();

                JSONObject jsonObject = new JSONObject(articleInfo);

                if (jsonObject.has("url")) {

                    String articleTitle = jsonObject.getString("title");
                    String articleURL = jsonObject.getString("url");
                    String articleTime = jsonObject.getString("time");

                    articleIDs.add(Integer.valueOf(articleID));
                    articleTitles.put(Integer.valueOf(articleID), articleTitle);
                    articleURLs.put(Integer.valueOf(articleID), articleURL);
                    articleTimes.put(Integer.valueOf(articleID), articleTime);

                    String sql = "INSERT INTO articles (articleID, url, title, articleTime) VALUES(?, ?, ?, ?)";

                    SQLiteStatement statement = articlesDB.compileStatement(sql);

                    statement.bindString(1, articleID);
                    statement.bindString(2, articleURL);
                    statement.bindString(3, articleTitle);
                    statement.bindString(4, articleTime);
                    statement.execute();

                }

               //Log.i("Article Info", "Title: " + articleTitle + " URL: " + articleURL);

            }

            Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);

            int articleIDIndex = c.getColumnIndex("articleID");
            int articleURLIndex = c.getColumnIndex("url");
            int articleTitleIndex = c.getColumnIndex("title");
            int articleTimeIndex = c.getColumnIndex("articleTime");

            //c.moveToFirst();

            if (c != null && c.moveToFirst()){

                do {

                    Log.i("DB Info articleID", Integer.toString(c.getInt(articleIDIndex)));
                    Log.i("DB Info articleURL", c.getString(articleURLIndex));
                    Log.i("DB Info articleTitle", c.getString(articleTitleIndex));
                    Log.i("DB Info articleTime", c.getString(articleTimeIndex));

                }while (c.moveToNext());
            }

            //Log.i("Article IDs", articleIDs.toString());
            //Log.i("Article Titles", articleTitles.toString());
            //Log.i("Article URls", articleURLs.toString());
            //Log.i("Article Times", articleTimes.toString());


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public class DownloadTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try{

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1){

                    char current = (char) data;
                    result += current;

                    data = reader.read();

                }

            }catch (Exception e){

                e.printStackTrace();
            }

            return result;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
