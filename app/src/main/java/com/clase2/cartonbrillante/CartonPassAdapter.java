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

public class CartonPassAdapter extends RecyclerView.Adapter<CartonPassAdapter.EncuentroViewHolder> {

    public interface OnEncuentroListener {
        void onEncuentroClick(EncuentroCartonPass encuentro);
        void onEncuentroLongClick(EncuentroCartonPass encuentro);
    }

    private List<EncuentroCartonPass> listaEncuentros;
    private final OnEncuentroListener listener;

    public CartonPassAdapter(List<EncuentroCartonPass> listaEncuentros, OnEncuentroListener listener) {
        this.listaEncuentros = listaEncuentros;
        this.listener = listener;
    }

    public void actualizarLista(List<EncuentroCartonPass> nuevaLista) {
        this.listaEncuentros = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EncuentroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_encuentro_cartonpass, parent, false);
        return new EncuentroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EncuentroViewHolder holder, int position) {
        EncuentroCartonPass item = listaEncuentros.get(position);

        holder.tvNick.setText(item.getNickname());
        holder.tvSaludo.setText("“" + item.getSaludo() + "”");
        holder.tvVeces.setText("x" + item.getVecesEncontrado());
        holder.tvFecha.setText("Visto: " + item.getFechaHora());

        String pais = (item.getPais() != null && !item.getPais().isEmpty()) ? item.getPais() : "Chile";
        String reg = (item.getRegulacion() != null && !item.getRegulacion().isEmpty()) ? item.getRegulacion() : "TCG";
        holder.tvPaisReg.setText(pais + " • " + reg);

        if ("OCG".equalsIgnoreCase(reg)) {
            holder.tvPaisReg.setBackgroundColor(Color.parseColor("#D32F2F")); // Rojo OCG
        } else {
            holder.tvPaisReg.setBackgroundColor(Color.parseColor("#5C6BC0")); // Azul TCG
        }

        byte[] avatar = item.getAvatarBytes();
        if (avatar != null && avatar.length > 0) {
            Bitmap bmp = BitmapFactory.decodeByteArray(avatar, 0, avatar.length);
            holder.ivAvatar.setImageBitmap(bmp);
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        String deck = (item.getDeckCompartido() == null || item.getDeckCompartido().isEmpty() || item.getDeckCompartido().equals("Ninguno (No mostrar)"))
                ? "Secreto / No especificado" : item.getDeckCompartido();
        holder.tvDeck.setText("🎴 Deck: " + deck);

        String inv = (item.getInvocacionPreferida() == null || item.getInvocacionPreferida().isEmpty() || item.getInvocacionPreferida().equals("Ninguna (No mostrar)"))
                ? "Sin preferencia" : item.getInvocacionPreferida();
        holder.tvInvocacion.setText("✨ Inv: " + inv);

        if (item.tieneUbicacionValida()) {
            String lugar = (item.getNombreLugar() != null && !item.getNombreLugar().isEmpty())
                    ? item.getNombreLugar() : String.format("%.4f, %.4f", item.getLatitud(), item.getLongitud());
            holder.tvUbicacion.setText("📍 " + lugar + " (En Mapa)");
            holder.tvUbicacion.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.tvUbicacion.setText("📍 Ubicación desactivada en el cruce");
            holder.tvUbicacion.setTextColor(Color.parseColor("#757575"));
        }

        holder.itemView.setOnClickListener(v -> listener.onEncuentroClick(item));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onEncuentroLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listaEncuentros.size();
    }

    static class EncuentroViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvNick, tvSaludo, tvDeck, tvInvocacion, tvPaisReg, tvVeces, tvUbicacion, tvFecha;

        public EncuentroViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivCpItemAvatar);
            tvNick = itemView.findViewById(R.id.tvCpItemNick);
            tvSaludo = itemView.findViewById(R.id.tvCpItemSaludo);
            tvDeck = itemView.findViewById(R.id.tvCpItemDeck);
            tvInvocacion = itemView.findViewById(R.id.tvCpItemInvocacion);
            tvPaisReg = itemView.findViewById(R.id.tvCpItemPaisReg);
            tvVeces = itemView.findViewById(R.id.tvCpItemVeces);
            tvUbicacion = itemView.findViewById(R.id.tvCpItemUbicacion);
            tvFecha = itemView.findViewById(R.id.tvCpItemFecha);
        }
    }
}