package com.trombonewind.ff14jobjudge;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements DialogInterface.OnClickListener, Runnable, AdapterView.OnItemClickListener, FileListDialog.OnDirSelectDialogListener {
    private static final int MAX_FACES = 20;
    private static final int THUMBNAIL_W = 80;
    private static final int THUMBNAIL_H= 60;


    private static final double AD_RATE = 0.333333333;

    private InterstitialAd interstitial;

    private ArrayAdapter<String> mArrayAdapter;
    private ListView mListView;
    private String mPicturesPath;
    private String[] mPicturesList;
    private ProgressDialog mProgressDialog;
    private Bitmap[] mThumbnails;
    private Gallery mGallery;
    private ImageView mImageView;
    private Bitmap mFdPicture;
    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker(getString(R.string.analytics_track_id));
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Create the interstitial.
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId(getString(R.string.Inter_ad_unit_id));
        AdRequest interadRequest = new AdRequest.Builder().build();

        // Begin loading your interstitial.
        interstitial.loadAd(interadRequest);


        mArrayAdapter = new ArrayAdapter<String>(this, R.layout.list_row, R.id.rowText);
        mArrayAdapter.clear();

        mListView = (ListView)findViewById(R.id.ListView01);
        mListView.setAdapter(mArrayAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    ListView listView = (ListView) parent;

                    String text = (String) listView.getItemAtPosition(position);
                    text = text.substring(9) + " " + getString(R.string.Google_Play_URL) + " #FF14JobJudge";

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(intent);
                } catch (Exception e) {
                    mProgressDialog = new ProgressDialog(MainActivity.this);
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("外部アプリケーションとの連携に失敗しました。")
                            .setNeutralButton("OK", MainActivity.this)
                            .show();
                    return;
                } finally {
                    displayInterstitial();
                }
            }
        });

    }

    // Invoke displayInterstitial() when you are ready to display an interstitial.
    public void displayInterstitial() {
        double rate = Math.random();
        if (interstitial.isLoaded() && rate < AD_RATE) {
            interstitial.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isFirstBoot()){
            setSecondBoot();
            FileListDialog dlg = new FileListDialog(this);
            //リスナーの登録
            dlg.setOnDirSelectDialogListener(this);
            //表示
            dlg.show(getFolderPath());
        } else {
            setGallaryContent(new File(getFolderPath()));
        }

        displayInterstitial();

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
        if (id == R.id.action_setting) {

            FileListDialog dlg = new FileListDialog(this);
            //リスナーの登録
            dlg.setOnDirSelectDialogListener(this);
            //表示
            dlg.show(getFolderPath());

        }

        if (id == R.id.action_howto) {
            showHowTo();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * ファイルリスト選択ハンドル
     */
    public void onClickDirSelect(File file) {
        if(file == null){
            Toast.makeText(this, "フォルダの取得ができませんでした", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this, file.getPath(), Toast.LENGTH_LONG).show();
            setFolderPath(file.getPath());
            setGallaryContent(file);
        }
    }



    // サムネイルを作成するスレッド
    @Override
    public void run() {
        int	w, h, scale_w, scale_h;

        for (int i = 0 ; i < mPicturesList.length ; i++) {
            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inJustDecodeBounds = true;	// 画像情報のみ読み込む
            BitmapFactory.decodeFile(mPicturesPath + mPicturesList[i], options);

            w = options.outWidth;
            h = options.outHeight;

            scale_w = w / THUMBNAIL_W;
            if ((w % THUMBNAIL_W) > 0) {
                scale_w++;
            }
            scale_h = h / THUMBNAIL_H;
            if ((h % THUMBNAIL_H) > 0) {
                scale_h++;
            }
            options.inSampleSize = Math.max(scale_w, scale_h);

            options.inJustDecodeBounds = false;
            mThumbnails[i] = BitmapFactory.decodeFile(mPicturesPath + mPicturesList[i], options);
        }
        mProgressDialog.dismiss();	// プログレスダイアログを閉じる
    }

    // アラートダイアログのボタンが押されたときに呼び出されるメソッド
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEUTRAL:
                mProgressDialog.dismiss();
        }
    }

    // ギャラリーの項目が選択されたときに呼び出されるメソッド
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        if (arg0 == mGallery) {
            mFdPicture = ExecFaceDetect(mPicturesPath + mPicturesList[arg2]);
            if (mFdPicture != null) {
                mImageView.setImageBitmap(mFdPicture);
            }
        }
    }

    // ギャラリーの選択肢に画像を設定するアダプタ
    public class GalleryImageAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mPicturesList.length;
        }
        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView	view;

            if (convertView == null) {
                view = new ImageView(MainActivity.this);
                view.setImageBitmap(mThumbnails[position]);
            } else {
                view = (ImageView)convertView;
            }
            return view;
        }
    }

    // SDカードが存在し読み出し可能かをチェック
    private boolean SDCardReadReady() {
        String	state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    // 顔検出の実行と検出結果画像の生成
    private Bitmap ExecFaceDetect(String FileName) {
        Bitmap					src;
        FaceDetector.Face[]		faces = new FaceDetector.Face[MAX_FACES];
        FaceDetector			detector;
        int						numFaces = 0;
        String					txt;

        src = BitmapFactory.decodeFile(FileName);
        src = src.copy(Bitmap.Config.RGB_565, true);

        mArrayAdapter.clear();
        detector = new FaceDetector(src.getWidth(), src.getHeight(), faces.length);

        numFaces = detector.findFaces(src, faces);		// 顔認識実行

        if (numFaces > 0) {
			/*
			 * 元のビットマップを複製し、認識した顔領域に赤い四角を描画
			 */
            Bitmap	newBitmap	= src.copy(Bitmap.Config.RGB_565, true);
            Canvas canvas		= new Canvas(newBitmap);
            Paint	paint		= new Paint();

            paint.setColor(Color.argb(255, 255, 0, 0));	// 赤
            paint.setStyle(Paint.Style.STROKE);				// 塗りつぶしなしの線

            // 認識した数だけ処理
            for (int i = 0 ; i < numFaces ; i++) {
                FaceDetector.Face face = faces[i];
                PointF midPoint = new PointF(0, 0);
                float	eyesDistance;
                RectF rect = new RectF();

                // 顔認識結果を取得
                face.getMidPoint(midPoint);				// 中心座標を取得
                eyesDistance = face.eyesDistance();		// 目の間隔を取得

                // 矩形描画
                rect.left	= midPoint.x - (eyesDistance);
                rect.top	= midPoint.y - (eyesDistance);
                rect.right	= midPoint.x + (eyesDistance);
                rect.bottom	= midPoint.y + (eyesDistance);
                canvas.drawRect(rect, paint);

                canvas.drawText("FACE[" + (i+1) + "]", midPoint.x - (eyesDistance), midPoint.y - (eyesDistance)-10, paint);

                txt = "FACE[" + (i+1) + "]: " + JudgeJob(face.confidence(), midPoint);
                mArrayAdapter.add(txt);

            }
            return( newBitmap );
        }
        return( src );
    }

    private void setGallaryContent(File dir){
        if (!dir.exists() && !dir.canRead()) {
            // 顔検出用の画像フォルダが見つからない...
            // アラートダイアログを表示して終了
            mProgressDialog = new ProgressDialog(this);
            new AlertDialog.Builder(this)
                    .setMessage(dir.getAbsolutePath() + "のフォルダが見つかりません。")
                    .setNeutralButton("OK", this)
                    .show();
            return;
        }
        mPicturesPath = dir.getAbsolutePath() + "/";
        mPicturesList = dir.list();
        if (mPicturesList != null && mPicturesList.length > 0) {
            // JPG形式 または PNG形式のみのファイル名一覧を構築
            List<String> tmp = new ArrayList<String>();
            for (String s: mPicturesList) {
                if (s.endsWith("JPG") || s.endsWith("jpg") ||
                        s.endsWith("PNG") || s.endsWith("png") ||
                        s.endsWith("JPEG") || s.endsWith("jpeg")) {
                    tmp.add(s);
                }
            }
            mPicturesList = tmp.toArray(new String[]{});
        } else {
            mProgressDialog = new ProgressDialog(this);
            new AlertDialog.Builder(this)
                    .setMessage(dir.getAbsolutePath() + "のフォルダにはファイルがありません")
                    .setNeutralButton("OK", this)
                    .show();
            return;
        }
        if (mPicturesList.length > 0) {

            // サムネイル作成中を示すプログレスダイアログ表示
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("サムネイルの取得中です。");
            mProgressDialog.show();

            // サムネイル作成には時間が掛かる可能性があるため、
            // 別スレッドにて処理する
            mThumbnails = new Bitmap[mPicturesList.length];
            new Thread(this).start();	// スレッド開始

            // ギャラリーの作成
            mGallery = (Gallery)findViewById(R.id.Gallery01);
            mGallery.setSpacing(2);
            mGallery.setAdapter(new GalleryImageAdapter());
            mGallery.setOnItemClickListener(this);

            // イメージビューに初期画像を設定
            mImageView = (ImageView)findViewById(R.id.ImageView01);

            mFdPicture = ExecFaceDetect(mPicturesPath + mPicturesList[0]);
            if (mFdPicture != null) {
                mImageView.setImageBitmap(mFdPicture);
            }
        } else {
            mProgressDialog = new ProgressDialog(this);
            new AlertDialog.Builder(this)
                    .setMessage(dir.getAbsolutePath() + "のフォルダには画像ファイルがありません")
                    .setNeutralButton("OK", this)
                    .show();
            return;
        }

    }

    private String JudgeJob(float confidence, PointF midpoint){

        float x = midpoint.x;
        float y = midpoint.y;

        Float resultF = confidence * 100 + x + y;

        String[] jobs = {"ナイト","戦士","暗黒騎士","モンク","竜騎士","忍者","黒魔道師",
                "召喚師","吟遊詩人","機工士","白魔道師","学者","占星術士","木工師",
                "鍛冶師","甲冑師","彫金師","革細工師","裁縫師","錬金術師","調理師",
                "採掘師","園芸師","漁師"};

        Integer resultI = Math.round(resultF) % 24;
        String result = "あなたに向いているジョブ・クラスは" + jobs[resultI] + "です！";

        return result;

    }


    private void setSecondBoot() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putBoolean(getString(R.string.sp_is_first),false).commit();
    }

    private boolean isFirstBoot() {
        // 読み込み
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(getString(R.string.sp_is_first) ,true);
    }

    private void setFolderPath(String path){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putString(getString(R.string.sp_saved_folder),path).commit();
    }

    private String getFolderPath(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(getString(R.string.sp_saved_folder), Environment.getExternalStorageDirectory().getPath());
    }

    private void showHowTo(){
        mProgressDialog = new ProgressDialog(this);
        new AlertDialog.Builder(this)
                .setMessage("[使い方]\n" +
                        "①メニューの「フォルダ選択」をタップ\n" +
                        "②画像のあるフォルダを選択\n" +
                        "③画面上部のサムネイルをタップ\n" +
                        "④中央に顔認識結果を表示\n" +
                        "⑤画面下部に判定結果が表示\n" +
                        "⑥判定結果をタップすると外部連携！")
                .setNeutralButton("閉じる",this)
                .show();

    }

}