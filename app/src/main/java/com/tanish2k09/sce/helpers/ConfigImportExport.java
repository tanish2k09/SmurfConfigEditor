package com.tanish2k09.sce.helpers;

import com.tanish2k09.sce.R;
import com.tanish2k09.sce.utils.ConfigCacheClass;
import com.tanish2k09.sce.utils.StringValClass;
import com.tanish2k09.sce.utils.TopCommentStore;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.io.SuFile;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static com.tanish2k09.sce.R.string.ConfigPath1;
import static com.tanish2k09.sce.R.string.ConfigPath2;

public class ConfigImportExport {
    private File configFile;
    private Context ctx;

    public ConfigImportExport(Context context) {
        ctx = context;
    }

    private int openConfig() {
        configFile = new File(Environment.getExternalStorageDirectory().getPath() + "/SmurfKernel",
                            ctx.getResources().getString(R.string.configFile));
        if (configFile.exists()) {
            return 0;
        } else {
            return -1;
        }
    }

    private boolean isValidConfigLine(String line) {
        return (line.split("=").length == 2);
    }

    /* The config file expects a certain notation/format:
     * The top value must be the profile.version
     *
     *  ##~ - Comment/Description
     *  ##* - Category
     *  ##: - Title
     *  # - Inactive Option (if it has exactly 1 '=')
     *  # - Comment (if not an inactive option)
     *  ##/ - Top comment (should be cached)
     */
    public boolean configImport() {
        if (openConfig() != 0) {
            if (configDumpRoot()) {
                Toast.makeText(ctx, ctx.getString(R.string.importRoot), Toast.LENGTH_SHORT).show();
                return configImport();
            }
            return false;
        } else {
            try {
                ConfigCacheClass.clearAll();
                BufferedReader inBR = new BufferedReader(new FileReader(configFile));
                String cache = inBR.readLine();
                String title = "";
                String category = "";
                StringBuilder description = new StringBuilder();

                while (cache != null) {
                    if (isValidConfigLine(cache)) {
                        String[] configPair;
                        if (cache.startsWith("#")) {
                            cache = cache.substring(1);
                            configPair = cache.split("=");
                            ConfigCacheClass.addConfig(configPair[0], configPair[1], false, title, description.toString(), category);
                        } else {
                            configPair = cache.split("=");
                            ConfigCacheClass.addConfig(configPair[0], configPair[1], true, title, description.toString(), category);
                        }
                        description = new StringBuilder();
                    } else if (cache.startsWith("##:")) {
                        if (cache.length() > 3)
                            title = cache.substring(3);
                    } else if (cache.startsWith("##~")) {
                            description.append(cache.substring(3)).append('\n');
                    } else if (cache.startsWith("##*")) {
                        if (cache.length() > 3)
                            category = cache.substring(3);
                    } else if (cache.startsWith("##/")) {
                        TopCommentStore.appendLine(cache);
                    }
                    cache = inBR.readLine();
                }
                return true;
            } catch (IOException e) {
                Toast.makeText(ctx, R.string.swwRC, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return false;
            }
        }
    }

    private String detectScript(int layer) {
        int MAX_CONFIG_POINTS = 2;
        if (layer > MAX_CONFIG_POINTS)
            return "";
        Log.d("SCE-CIE", " -- Checking layer " + layer);

        String path = "";

        if (layer == 1)
            path = ctx.getResources().getString(ConfigPath1);
        else if (layer == 2)
            path = ctx.getResources().getString(ConfigPath2);

        SuFile script = new SuFile(path);
        if (script.exists())
            return path;

        return detectScript(++layer);
    }

    private void runScript() {
        if (!Shell.rootAccess()) {
            Toast.makeText(ctx, ctx.getResources().getString(R.string.noRoot), Toast.LENGTH_LONG).show();
            Log.e("SCE-CIE", "--- NO ROOT ACCESS ---");
            return;
        }
        Log.d("SCE-CIE", "--- RunScript true ---");
        String scriptPath = detectScript(1);

        bashSh(scriptPath);
    }

    private void bashSh(String path) {
        Log.d("SCE-CIE", "Executing script path: " + path);
        Shell.su("sh " + path).submit();
    }

    public void saveConfig(boolean runScript) {
        try {
            Log.d("SCE-CIE", "New config file created? " + configFile.createNewFile());

            BufferedWriter outBW = new BufferedWriter(new FileWriter(configFile));
            outBW.write(TopCommentStore.getComment());

            for (int idx = 0; idx < ConfigCacheClass.getConfiglistSize(); ++idx) {
                StringValClass svc = ConfigCacheClass.getStringVal(idx);
                assert svc != null;

                String category = svc.getCategory();

                if (category.length() > 0 && !svc.getName().equals(ctx.getResources().getString(R.string.profileVersion))) {
                    outBW.write("##*" + category);
                    outBW.newLine();
                }

                if (svc.getTitle().length() > 0) {
                    outBW.write("##:" + svc.getTitle());
                    outBW.newLine();
                }

                if (svc.getDescriptionString().length() > 0) {
                    String[] lines = svc.getDescriptionString().split("\n");
                    for (String string : lines) {
                        outBW.write("##~" + string);
                        outBW.newLine();
                    }
                }

                for (int option = 0; option < svc.getNumOptions(); ++option) {
                    StringBuilder lineToWrite = new StringBuilder();

                    if (!svc.getOption(option).equals(svc.getActiveVal()) &&
                        !svc.getName().equals(ctx.getString(R.string.profileVersion)))
                            lineToWrite.append("#");

                    lineToWrite.append(svc.getName()).append("=").append(svc.getOption(option));
                    outBW.write(lineToWrite.toString());
                    outBW.newLine();
                }
                outBW.newLine();
                outBW.newLine();
            }
            outBW.flush();
            outBW.close();
            Toast.makeText(ctx, "Saved successfully", Toast.LENGTH_SHORT).show();
            if (runScript)
                runScript();
        } catch (IOException e) {
            Toast.makeText(ctx, R.string.swwRC, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean configDumpRoot() {
        String filename = ctx.getResources().getString(R.string.configFile);
        File rootConfig = SuFile.open("/"+ filename);
        if (rootConfig.exists())
            Shell.su("cp /" + filename + " " + configFile.getAbsolutePath()).exec();
        return configFile.exists();
    }
}
