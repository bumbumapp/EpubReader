package com.folioreader.android.sample;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.folioreader.Config;
import com.folioreader.Constants;
import com.folioreader.FolioReader;
import com.folioreader.model.HighLight;
import com.folioreader.ui.view.LoadingView;
import com.folioreader.util.AppUtil;
import com.folioreader.util.OnHighlightListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.readium.r2.shared.Publication;
import org.readium.r2.streamer.parser.PubBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class EpubAdabter extends RecyclerView.Adapter<EpubAdabter.ViewHolder> {
    Context context;
    ArrayList<String> arrayList=new ArrayList();
    ArrayList<String> arrayListmodified=new ArrayList();
    ClickListener clickListener;
    public EpubAdabter(Context context, ArrayList<String> arrayList,ArrayList<String> arrayListmodified,ClickListener clickListener){
        this.context=context;
        this.arrayList=arrayList;
        this.clickListener=clickListener;
        this.arrayListmodified=arrayListmodified;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.item_books,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
            holder.title.setText(arrayList.get(position).substring(arrayList.get(position).lastIndexOf("/")+1));
            holder.openFile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onClick(position);
                }
            });
            holder.modified.setText("Last modified "+arrayListmodified.get(position));

//             arrayListLink=getArrayList();
//        try {
//
//           holder.imageView.setImageBitmap( getBitmapFromURL(arrayList.get(position)));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        Log.d("Tag","TAGW"+getArrayList());
//            Log.d("Tag","array "+arrayList.get(position));
//        Bitmap bm = null;



    }




    @Override
    public int getItemCount() {
        return arrayList.size();
    }




    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title,modified;
        LinearLayout openFile;
        ImageView imageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title=itemView.findViewById(R.id.book_title);
            imageView=itemView.findViewById(R.id.book_cover);
            openFile=itemView.findViewById(R.id.top);
            modified=itemView.findViewById(R.id.book_modififed);
        }
    }


}
