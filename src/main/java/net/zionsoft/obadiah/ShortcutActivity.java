package net.zionsoft.obadiah;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ShortcutActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, new Intent(this, BookSelectionActivity.class));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher));
        setResult(RESULT_OK, intent);

        finish();
    }
}
