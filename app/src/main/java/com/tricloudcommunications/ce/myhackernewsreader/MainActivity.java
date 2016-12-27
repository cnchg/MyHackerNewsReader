package com.tricloudcommunications.ce.myhackernewsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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

    ListView articleLV;
    ArrayList<String> articleTitleList;
    ArrayAdapter arrayAdapter;

    ArrayList<String> articleURLList;
    //ArrayList<String> articleContentList; //Used for IF we were downloading the URL HTML Content


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        articleLV = (ListView) findViewById(R.id.articleListView);
        articleTitleList = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, articleTitleList);
        articleLV.setAdapter(arrayAdapter);
        articleURLList = new ArrayList<String>();
        //articleContentList = new ArrayList<String>(); //Used for IF we were downloading the URL HTML Content

        articleLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("articleURL", articleURLList.get(position));
                //i.putExtra("articleContent", articleContentList.get(position)); //Used for IF we were downloading the URL HTML Content
                startActivity(i);

                Log.i("Article URLs", articleURLList.get(position));

            }
        });

        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleID INTEGER, url VARCHAR, title VARCHAR, content VARCHAR, articleTime VARCHAR)");

        updateListView();

        DownloadTask task = new DownloadTask();
        try {

            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void updateListView(){

        try {

            Log.i("UI Updated", "DONE");
            Cursor c = articlesDB.rawQuery("SELECT * FROM articles ORDER BY articleID DESC", null);

            int articleIDIndex = c.getColumnIndex("articleID");
            int articleURLIndex = c.getColumnIndex("url");
            int articleTitleIndex = c.getColumnIndex("title");
            int articleTimeIndex = c.getColumnIndex("articleTime");
            //int articleContentIndex = c.getColumnIndex("content"); //Used for IF we were downloading the URL HTML Content

            articleTitleList.clear();
            articleURLList.clear();

            if (c != null && c.moveToFirst()){

                do {

                    articleTitleList.add(c.getString(articleTitleIndex));
                    articleURLList.add(c.getString(articleURLIndex));
                    //articleContentList.add(c.getString(articleContentIndex)); //Used for IF we were downloading the URL HTML Content

                    //Log.i("DB Info articleID", Integer.toString(c.getInt(articleIDIndex)));
                    //Log.i("DB Info articleURL", c.getString(articleURLIndex));
                    //Log.i("DB Info articleTitle", c.getString(articleTitleIndex));
                    //Log.i("DB Info articleTime", c.getString(articleTimeIndex));

                }while (c.moveToNext());
            }

            arrayAdapter.notifyDataSetChanged();
        }catch (Exception e){

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

                JSONArray jsonArray = new JSONArray(result);

                articlesDB.execSQL("DELETE FROM articles");

                /** Use this for loop funtion if you wanted to get all 500 articles ID's
                 for(int i = 0; i < 20; i++){
                 Log.i("Article ID", jsonArray.getString(i));
                 }
                 **/

                //In this case we only want to get 20 article ID's
                for(int i = 0; i < 20; i++){

                    String articleID = jsonArray.getString(i);
                    //Log.i("Article ID", jsonArray.getString(i));

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleID+".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();

                    String articleInfo = "";

                    while (data != -1){

                        char current = (char) data;

                        articleInfo += current;

                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (jsonObject.has("url")) {

                        String articleTitle = jsonObject.getString("title");
                        String articleURL = jsonObject.getString("url");
                        String articleTime = jsonObject.getString("time");
                        //Log.i("Article Info", "Title: " + articleTitle + " URL: " + articleURL);

                        /**
                         *  //Used for IF we were downloading the URL HTML Content
                        url = new URL(articleURL);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        in = urlConnection.getInputStream();
                        reader = new InputStreamReader(in);
                        data = reader.read();

                        String articleContent = "";

                        while (data != -1){

                            char current = (char) data;

                            articleContent += current;

                            data = reader.read();
                        }
                         */

                        articleIDs.add(Integer.valueOf(articleID));
                        articleTitles.put(Integer.valueOf(articleID), articleTitle);
                        articleURLs.put(Integer.valueOf(articleID), articleURL);
                        articleTimes.put(Integer.valueOf(articleID), articleTime);

                        //String sql = "INSERT INTO articles (articleID, url, title, articleTime, content) VALUES(?, ?, ?, ?, ?)"; //Used for IF we were downloading the URL HTML Content

                        String sql = "INSERT INTO articles (articleID, url, title, articleTime) VALUES(?, ?, ?, ?)";

                        SQLiteStatement statement = articlesDB.compileStatement(sql);

                        statement.bindString(1, articleID);
                        statement.bindString(2, articleURL);
                        statement.bindString(3, articleTitle);
                        statement.bindString(4, articleTime);
                        //statement.bindString(5, articleContent); //Used for IF we were downloading the URL HTML Content
                        statement.execute();

                    }

                    //Log.i("Article IDs", articleIDs.toString());
                    //Log.i("Article Titles", articleTitles.toString());
                    //Log.i("Article URls", articleURLs.toString());
                    //Log.i("Article Times", articleTimes.toString());

                }

            }catch (Exception e){

                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
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
