package com.chatlayoutexample;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.chatlayoutexample.R.layout.activity_oobprocessor;

public class OOBProcessor extends ActionBarActivity {

    ChatActivity activity;
    public OOBProcessor(ChatActivity activity) {

        this.activity = activity;

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_oobprocessor);}

        // remove the oob tags and send it along to the processor

    public String removeOobTags(String output) throws Exception {

        if (output != null) {

            Pattern pattern = Pattern.compile("<oob>(.*)</oob>");

            Matcher matcher = pattern.matcher(output);

            if (matcher.find()) {

                String oobContent = matcher.group(1);

                processInnerOobTags(oobContent);
                return oobContent;

            }

        }
        return "Processing";

    }


    // check inner oob command and take appropriate action

    public void processInnerOobTags(String oobContent) throws Exception {

        if (oobContent.contains("<dial>")) {

            oobDial();

        }


    }


    // dial a number

    public void oobDial() {
        Uri number = Uri.parse("tel:025246000");
        Intent callIntent = new Intent(Intent.ACTION_DIAL, number);
        startActivity(callIntent);

    }


}


