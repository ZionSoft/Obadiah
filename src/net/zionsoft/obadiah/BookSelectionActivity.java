package net.zionsoft.obadiah;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class BookSelectionActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_bookselection_activity);

        BibleReader bibleReader = BibleReader.getInstance();
        // NOTE must call this before BibleReader is further used
        bibleReader.setRootDir(getFilesDir());

        // loads the translation as used last time
        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        String selectedTranslation = null;
        try {
            selectedTranslation = preferences.getString("selectedTranslation", null);
        } catch (ClassCastException e) {
            // the value is an integer for 1.0 and 1.1
            int selected = preferences.getInt("selectedTranslation", -1);
            if (selected == 0)
                selectedTranslation = "authorized-king-james";
            else if (selected == 1)
                selectedTranslation = "chinese-union-simplified";
        }
        bibleReader.selectTranslation(selectedTranslation);

        // initializes the view for book names
        GridView gridView = (GridView) findViewById(R.id.gridView);
        gridView.setColumnWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 144, getResources()
                .getDisplayMetrics()));
        m_listAdapter = new SelectionListAdapter(this);
        gridView.setAdapter(m_listAdapter);
        gridView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                startChapterSelectionActivity(false, position);
            }
        });

        // initialize the menu button handler
        Button button = (Button) findViewById(R.id.menuButton);
        button.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(BookSelectionActivity.this);
                builder.setTitle(R.string.dialog_menu_title);

                CharSequence[] items = null;
                Resources resources = BookSelectionActivity.this.getResources();
                if (m_canContinueReading) {
                    items = new CharSequence[2];
                    items[0] = resources.getText(R.string.menu_select_translation);
                    items[1] = resources.getText(R.string.menu_continue_reading);
                } else {
                    // checks if from ChapterSelectionActivity or first boot
                    // i.e. not from TranslationSelectionActivity
                    if (!m_fromTranslationSelectionActivity
                            && getSharedPreferences("settings", MODE_PRIVATE).getInt("currentBook", -1) >= 0) {
                        items = new CharSequence[2];
                        items[0] = resources.getText(R.string.menu_select_translation);
                        items[1] = resources.getText(R.string.menu_continue_reading);
                        m_canContinueReading = true;
                    } else {
                        items = new CharSequence[1];
                        items[0] = resources.getText(R.string.menu_select_translation);
                    }
                }
                builder.setItems(items, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();

                        switch (which) {
                        case 0: // select translation
                            startTranslationSelectionActivity();
                            break;
                        case 1: // continue reading
                            startChapterSelectionActivity(true,
                                    getSharedPreferences("settings", MODE_PRIVATE).getInt("currentBook", 0));
                            break;
                        }
                    }
                });

                builder.create().show();
            }
        });
    }

    protected void onResume()
    {
        super.onResume();

        // opens dialog if no translation installed
        // the code is here in case the user doesn't download anything
        final TranslationInfo[] installedTranslations = BibleReader.getInstance().installedTranslations();
        if (installedTranslations == null || installedTranslations.length == 0) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(R.string.text_no_translation).setCancelable(false)
                    .setPositiveButton(R.string.text_yes, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.dismiss();
                            startTranslationSelectionActivity();
                        }
                    }).setNegativeButton(R.string.text_no, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.cancel();
                            BookSelectionActivity.this.finish();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            return;
        }

        // updates the title and texts
        // TODO no need to update if selected translation is not changed
        TranslationInfo translationInfo = BibleReader.getInstance().selectedTranslation();
        if (translationInfo == null)
            return;
        if (m_titleTextView == null)
            m_titleTextView = (TextView)findViewById(R.id.titleText);
        m_titleTextView.setText(translationInfo.name);
        m_listAdapter.setTexts(translationInfo.bookName);
    }

    private void startChapterSelectionActivity(boolean continueReading, int selectedBook)
    {
        m_fromTranslationSelectionActivity = false;

        Intent intent = new Intent(this, ChapterSelectionActivity.class);
        intent.putExtra("continueReading", continueReading);
        intent.putExtra("selectedBook", selectedBook);
        startActivity(intent);
    }

    private void startTranslationSelectionActivity()
    {
        m_fromTranslationSelectionActivity = true;

        Intent intent = new Intent(this, TranslationSelectionActivity.class);
        startActivity(intent);
    }

    private boolean m_fromTranslationSelectionActivity;
    private boolean m_canContinueReading;
    private SelectionListAdapter m_listAdapter;
    private TextView m_titleTextView;
}
