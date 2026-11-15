package com.clase2.cartonbrillante;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CarpetaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_AGREGAR = 0;
    private static final int VIEW_TYPE_CARTA = 1;

    public interface OnCartaInteractionListener {
        void onAgregarNuevaCartaClick();
        void onCartaClick(CartaCarpeta carta);
        void onCartaLongClick(CartaCarpeta carta);
    }

    private List<CartaCarpeta> listaCartas;
    private OnCartaInteractionListener listener;

    public CarpetaAdapter(List<CartaCarpeta> listaCartas, OnCartaInteractionListener listener) {
        this.listaCartas = listaCartas;
        this.listener = listener;
    }

    public void actualizarLista(List<CartaCarpeta> nuevaLista) {
        this.listaCartas = nuevaLista;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_AGREGAR : VIEW_TYPE_CARTA;
    }

    @Override
    public int getItemCount() {
        return listaCartas.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_AGREGAR) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carta_agregar, parent, false);
            return new AgregarCartaViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carta_carpeta, parent, false);
            return new CartaViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_AGREGAR) {
            holder.itemView.setOnClickListener(v -> listener.onAgregarNuevaCartaClick());
        } else {
            CartaCarpeta carta = listaCartas.get(position - 1);
            CartaViewHolder cartaHolder = (CartaViewHolder) holder;

            cartaHolder.tvNombreCarta.setText(carta.getNombre());
            cartaHolder.tvSetRareza.setText(carta.getCodigoSet() + " • " + carta.getRareza());
            cartaHolder.tvCantidadBadge.setText("x" + carta.getCantidad());

            String reg = (carta.getRegulacion() != null) ? carta.getRegulacion() : "TCG";
            cartaHolder.tvRegulacionCartaBadge.setText(reg);
            if ("OCG".equalsIgnoreCase(reg)) {
                cartaHolder.tvRegulacionCartaBadge.setBackgroundColor(Color.parseColor("#D32F2F"));
            } else {
                cartaHolder.tvRegulacionCartaBadge.setBackgroundColor(Color.parseColor("#5C6BC0"));
            }

            if (carta.getPrecioReferencia() > 0) {
                cartaHolder.tvPrecioCondicion.setText("$" + carta.getPrecioReferencia() + " (" + carta.getCondicion() + ")");
            } else {
                cartaHolder.tvPrecioCondicion.setText("Cambio (" + carta.getCondicion() + ")");
            }

            byte[] blob = carta.getImagen();
            if (blob != null && blob.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.length);
                cartaHolder.ivImagenCarta.setImageBitmap(bitmap);
            } else {
                cartaHolder.ivImagenCarta.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            cartaHolder.itemView.setOnClickListener(v -> listener.onCartaClick(carta));
            cartaHolder.itemView.setOnLongClickListener(v -> {
                listener.onCartaLongClick(carta);
                return true;
            });
        }
    }

    static class AgregarCartaViewHolder extends RecyclerView.ViewHolder {
        public AgregarCartaViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class CartaViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImagenCarta;
        TextView tvRegulacionCartaBadge, tvCantidadBadge, tvNombreCarta, tvSetRareza, tvPrecioCondicion;

        public CartaViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImagenCarta = itemView.findViewById(R.id.ivImagenCarta);
            tvRegulacionCartaBadge = itemView.findViewById(R.id.tvRegulacionCartaBadge);
            tvCantidadBadge = itemView.findViewById(R.id.tvCantidadBadge);
            tvNombreCarta = itemView.findViewById(R.id.tvNombreCarta);
            tvSetRareza = itemView.findViewById(R.id.tvSetRareza);
            tvPrecioCondicion = itemView.findViewById(R.id.tvPrecioCondicion);
        }
    }
}