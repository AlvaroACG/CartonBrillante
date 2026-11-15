package com.clase2.cartonbrillante;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeckFotosAdapter extends RecyclerView.Adapter<DeckFotosAdapter.FotoViewHolder> {

    public interface OnFotoClickListener {
        void onFotoClick(int position);
    }

    private final List<byte[]> listaFotos;
    private final OnFotoClickListener listener;

    public DeckFotosAdapter(List<byte[]> listaFotos, OnFotoClickListener listener) {
        this.listaFotos = listaFotos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        float density = context.getResources().getDisplayMetrics().density;

        ImageView imageView = new ImageView(context);
        int ancho = (int) (54 * density);
        int alto = (int) (78 * density);
        int margen = (int) (3 * density);

        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ancho, alto);
        params.setMargins(margen, margen, margen, margen);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundColor(0xFF27273A);

        return new FotoViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        byte[] bytes = listaFotos.get(position);
        if (bytes != null && bytes.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            holder.imageView.setImageBitmap(bmp);
        }
        holder.imageView.setOnClickListener(v -> {
            int posActual = holder.getAdapterPosition();
            if (posActual != RecyclerView.NO_POSITION) {
                listener.onFotoClick(posActual);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaFotos.size();
    }

    static class FotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public FotoViewHolder(@NonNull ImageView itemView) {
            super(itemView);
            this.imageView = itemView;
        }
    }
}