package com.chatlayoutexample;







import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.*;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import static java.lang.Thread.sleep;

public class ChatActivity extends ActionBarActivity {

    private EditText messageET;
    private ListView messagesContainer;
    private Button sendBtn;
    private ChatAdapter adapter;
    private static final String TAG = "MainActivity";
    ArrayList<ChatMessage> chatHistory = new ArrayList<ChatMessage>();
    String botname = "ait";
    Chat chatSession=null;
    TextToSpeech t1;

    boolean paused=true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        File fileExt = new File(getExternalFilesDir("new1").getAbsolutePath() + "/bots");
        if (!fileExt.exists()) {
            ZipFileExtraction extract = new ZipFileExtraction();

            try {
                extract.unZipIt(getAssets().open("bots.zip"), getExternalFilesDir(null).getAbsolutePath() + "/");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        initControls();

        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                    t1.speak("Hi, How can I help you?", TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });




    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }
    protected void sendMessage(final String message) {

        String response = doInBackground(message);
        String responsetoprint = response;
        //if any oob tags change response to print
        if (response != null) {

            Pattern pattern = Pattern.compile("(.*)<oob>(.*)</oob>");

            Matcher matcher = pattern.matcher(response);

            if (matcher.find()) {

                responsetoprint = matcher.group(1);

            }

        }
        processBotResponse(response);
        onPostExecute(responsetoprint);
        if(!responsetoprint.equals("...")) {
            t1.speak(responsetoprint, TextToSpeech.QUEUE_FLUSH, null);
        }

    }

     String doInBackground(String message) {
                final String path = getExternalFilesDir(null).getAbsolutePath();

                Bot bot = new Bot(botname, path);
                if(chatSession==null)
                {chatSession = new Chat(bot);}

                String request = message;


                //String request = "What is your name?";
                String response = chatSession.multisentenceRespond(request);

         Log.v(TAG, "response = " + response);



                return response;
            }



            public String processBotResponse(String response) {

            if (response.contains("<oob>")) {

                try {

                    removeOobTags(response);

                } catch (Exception e) {

                    e.printStackTrace();

                }

            }

                return response;
            }
            protected void onPostExecute(String response) {
                if (response.isEmpty()) {
                    response = "There is no response";
                }else {
                    ChatMessage msg1 = new ChatMessage();
                    msg1.setId(2);
                    msg1.setMe(false);
                    msg1.setMessage(response);
                    msg1.setDate(DateFormat.getDateTimeInstance().format(new Date()));
                    chatHistory.add(msg1);
                    ChatMessage message = chatHistory.get(0);
                    displayMessage(msg1);
                }
            }



    public void removeOobTags(String output) throws Exception {

        if (output != null) {

            Pattern pattern = Pattern.compile("(.*)<oob>(.*)</oob>");

            Matcher matcher = pattern.matcher(output);

            if (matcher.find()) {

                String oobContent = matcher.group(2);

                processInnerOobTags(oobContent);


            }

        }


    }


    // check inner oob command and take appropriate action

    public void processInnerOobTags(String oobContent) throws Exception {

        if (oobContent.contains("<dial>")) {
            Pattern pattern = Pattern.compile("<dial>(.*)</dial>");

            Matcher matcher = pattern.matcher(oobContent);

            if (matcher.find()) {

                String phonenumber = matcher.group(1);
                oobDial(phonenumber);

            }


        }
        if (oobContent.contains("<search>")) {
            Pattern pattern = Pattern.compile("<search>(.*)</search>");

            Matcher matcher = pattern.matcher(oobContent);

            if (matcher.find()) {

                String searchstring = matcher.group(1);
                oobSearch(searchstring);

            }


        }
        if (oobContent.contains("<map>")) {
            Pattern pattern = Pattern.compile("<map>(.*)</map>");

            Matcher matcher = pattern.matcher(oobContent);

            if (matcher.find()) {

                String destination = matcher.group(1);
                oobMaps(destination);

            }


        }
    }
    public void oobMaps(String destination) {
        String uri = "geo:0,0?q="+destination;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        try
        {
            startActivity(intent);
        }
        catch(ActivityNotFoundException ex)
        {
            try
            {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(unrestrictedIntent);
            }
            catch(ActivityNotFoundException innerEx)
            {
                Toast.makeText(this, "Please install a maps application", Toast.LENGTH_LONG).show();
            }
        }
    }



    public void oobSearch(String query) {

        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        intent.putExtra(SearchManager.QUERY, query);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);


    }
    // dial a number

    public void oobDial(String phonenumber) {
        String numberformat="tel:"+phonenumber;

    Uri number = Uri.parse(numberformat);
        Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
        startActivity(callIntent);

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

    private void initControls() {
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        messageET = (EditText) findViewById(R.id.messageEdit);
        sendBtn = (Button) findViewById(R.id.chatSendButton);

        TextView meLabel = (TextView) findViewById(R.id.meLbl);
        TextView companionLabel = (TextView) findViewById(R.id.friendLabel);
        RelativeLayout container = (RelativeLayout) findViewById(R.id.container);


        loadDummyHistory();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageET.getText().toString();
                if (TextUtils.isEmpty(messageText)) {
                    return;
                }

                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setId(122);//dummy
                chatMessage.setMessage(messageText);
                chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
                chatMessage.setMe(true);

                messageET.setText("");

                displayMessage(chatMessage);
                sendMessage(messageText);
            }
        });


    }

    public void displayMessage(ChatMessage message) {
        adapter.add(message);
        adapter.notifyDataSetChanged();
        scroll();
    }

    private void scroll() {
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    private void loadDummyHistory(){



        ChatMessage msg = new ChatMessage();
        msg.setId(1);
        msg.setMe(false);
        msg.setMessage("Hi");
        msg.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        chatHistory.add(msg);
        ChatMessage msg1 = new ChatMessage();
        msg1.setId(2);
        msg1.setMe(false);
        msg1.setMessage("How can I help you?");
        msg1.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        chatHistory.add(msg1);
        ChatMessage msg2 = new ChatMessage();
        msg2.setId(3);
        msg2.setMe(false);
        msg2.setMessage("Ask me anything about your stay at AIT, I am at your service! To know more about the services I offer, say 'help'");
        msg2.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        chatHistory.add(msg2);


        adapter = new ChatAdapter(ChatActivity.this, new ArrayList<ChatMessage>());
        messagesContainer.setAdapter(adapter);

                for(int i=0; i<chatHistory.size(); i++) {
                    ChatMessage message = chatHistory.get(i);
                    displayMessage(message);

                }

    }


}
