package it.syscake.notificationlistenerlibrary.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import it.syscake.notificationlistenerlibrary.R;
import it.syscake.notificationlistenerlibrary.SharedPrefManager;

public class PackagesAdapter extends RecyclerView.Adapter<PackagesAdapter.PackagesViewHolder> {

    public final ArrayList<ApplicationInfo> packages = new ArrayList<>(0);
    private final Context context;

    public PackagesAdapter(Context context) {
        this.context = context;
    }

    public void setDataset(List<ApplicationInfo> newPackages) {
        if (newPackages == null)
            return;

        packages.clear();
        for (ApplicationInfo ai : newPackages) {
            if (!isSystemPackage(ai.flags))
                packages.add(ai);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            packages.sort((o, oo) -> {
                String s = o.loadLabel(context.getPackageManager()).toString();
                String ss = oo.loadLabel(context.getPackageManager()).toString();
                return s.compareToIgnoreCase(ss);
            });
        }

        notifyDataSetChanged();
    }

    private boolean isSystemPackage(int flags) {
        return ((flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    @NonNull
    @Override
    public PackagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(context).inflate(R.layout.packages_item, parent, false);
        return new PackagesViewHolder(rootView);
    }

    @Override
    public void onBindViewHolder(@NonNull PackagesViewHolder holder, int position) {
        ApplicationInfo applicationInfo = packages.get(position);
        holder.app_name.setText(applicationInfo.loadLabel(context.getPackageManager()));
        holder.app_icon.setImageDrawable(applicationInfo.loadIcon(context.getPackageManager()));
        SharedPrefManager instance = SharedPrefManager.getInstance(context);
        holder.enable_notifications.setChecked(instance.getPackageEnabled(applicationInfo.packageName));
        holder.enable_notifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                instance.enablePackage(applicationInfo.packageName);
            } else {
                instance.disablePackage(applicationInfo.packageName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return packages.size();
    }

    public static class PackagesViewHolder extends RecyclerView.ViewHolder {
        ImageView app_icon;
        TextView app_name;
        SwitchCompat enable_notifications;

        public PackagesViewHolder(@NonNull View itemView) {
            super(itemView);
            app_icon = itemView.findViewById(R.id.app_icon);
            app_name = itemView.findViewById(R.id.app_name);
            enable_notifications = itemView.findViewById(R.id.enable_notifications);
        }
    }
}
