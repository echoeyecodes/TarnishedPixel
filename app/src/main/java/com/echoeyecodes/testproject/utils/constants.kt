package com.echoeyecodes.testproject.utils

import android.content.res.Resources


fun getScreenSize():Pair<Int, Int>{
    return Pair(Resources.getSystem().displayMetrics.widthPixels, Resources.getSystem().displayMetrics.heightPixels)
}
