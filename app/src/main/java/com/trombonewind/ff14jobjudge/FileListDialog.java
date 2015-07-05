package com.trombonewind.ff14jobjudge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ディレクトリ選択ダイアログ
 */
public class FileListDialog extends Activity implements OnClickListener {

    /**
     * アクティビティ
     */
    private Activity activity = null;

    /**
     * リスナー
     */
    private OnDirSelectDialogListener listener = null;

    /**
     * ファイル情報
     */
    private File fileData = null;

    /**
     * 表示中のファイル情報リスト
     */
    private List<File> viewFileDataList = null;

    /**
     * コントラクト
     *
     * @param activity アクティビティ
     */
    public FileListDialog(Activity activity) {

        this.activity = activity;
    }

    public FileListDialog(){
        super();
    }

    /**
     * 選択イベント
     *
     * @param dialog ダイアログ
     * @param which  選択位置
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {

        show(this.viewFileDataList.get(which).getAbsolutePath() + "/");
    }

    /**
     * ダイアログを表示
     *
     * @param dirPath ディレクトリのパス
     */
    public void show(final String dirPath) {

        // ファイル情報
        this.fileData = new File(dirPath);

        // ファイルリスト
        File[] fileArray = this.fileData.listFiles();

        // 名前リスト
        List<String> nameList = new ArrayList<String>();

        if (fileArray != null) {

            // ファイル情報マップ
            Map<String, File> map = new HashMap<String, File>();

            for (File file : fileArray) {

                if (file.isDirectory()) {

                    nameList.add(file.getName() + "/");
                    map.put(nameList.get(map.size()), file);
                }
            }

            // ソート
            Collections.sort(nameList);

            // ファイル情報リスト
            this.viewFileDataList = new ArrayList<File>();

            for (int i = 0; i < nameList.size(); i++) {

                this.viewFileDataList.add(map.get(nameList.get(i)));
            }
        }

        // ダイアログを生成
        AlertDialog.Builder dialog = new AlertDialog.Builder(this.activity);
        dialog.setTitle(dirPath);
        dialog.setItems(nameList.toArray(new String[0]), this);

        dialog.setPositiveButton("決 定", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int value) {

                FileListDialog.this.listener.onClickDirSelect(FileListDialog.this.fileData);
            }
        });

        dialog.setNeutralButton("上 へ", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int value) {

                if (!"/".equals(dirPath)) {

                    String dirPathNew = dirPath.substring(0, dirPath.length() - 1);
                    dirPathNew = dirPathNew.substring(0, dirPathNew.lastIndexOf("/") + 1);

                    // 1つ上へ
                    show(dirPathNew);

                } else {

                    // 現状維持
                    show(dirPath);
                }
            }
        });

        dialog.setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int value) {

                FileListDialog.this.listener.onClickDirSelect(null);
            }
        });

        dialog.show();
    }

    /**
     * リスナーを設定
     *
     * @param listener 選択イベントリスナー
     */
    public void setOnDirSelectDialogListener(OnDirSelectDialogListener listener) {

        this.listener = listener;
    }

    /**
     * ボタン押下インターフェース
     */
    public interface OnDirSelectDialogListener {

        /**
         * 選択イベント
         *
         * @param file ファイル
         */
        void onClickDirSelect(File file);
    }
}
