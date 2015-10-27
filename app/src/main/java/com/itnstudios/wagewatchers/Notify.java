package com.itnstudios.wagewatchers;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by shlomo on 10/22/15.
 */
public class Notify {
    public static void viaToast(Context context, String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
