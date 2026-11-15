package com.clase2.cartonbrillante;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DecksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_AGREGAR = 0;
    private static final int VIEW_TYPE_DECK = 1;

    public interface OnDeckInteractionListener {
        void onAgregarNuevoClick();
        void onDeckClick(Deck deck);
        void onDeckLongClick(Deck deck);
    }

    private List<Deck> listaDecks;
    private OnDeckInteractionListener listener;

    public DecksAdapter(List<Deck> listaDecks, OnDeckInteractionListener listener) {
        this.listaDecks = listaDecks;
        this.listener = listener;
    }

    public void actualizarLista(List<Deck> nuevaLista) {
        this.listaDecks = nuevaLista;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_AGREGAR : VIEW_TYPE_DECK;
    }

    @Override
    public int getItemCount() {
        return listaDecks.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_AGREGAR) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck_agregar, parent, false);
            return new AgregarViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck_guardado, parent, false);
            return new DeckViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_AGREGAR) {
            holder.itemView.setOnClickListener(v -> listener.onAgregarNuevoClick());
        } else {
            Deck deck = listaDecks.get(position - 1);
            DeckViewHolder deckHolder = (DeckViewHolder) holder;

            deckHolder.tvNombre.setText(deck.getNombreDeck());

            String estadoVisibilidad = deck.isEsPublico() ? "🌐" : "🔒";
            String reg = (deck.getRegulacion() != null) ? deck.getRegulacion() : "TCG";
            deckHolder.tvRegulacion.setText(reg + " " + estadoVisibilidad);

            if ("OCG".equalsIgnoreCase(reg)) {
                deckHolder.tvRegulacion.setBackgroundColor(Color.parseColor("#D32F2F"));
            } else {
                deckHolder.tvRegulacion.setBackgroundColor(Color.parseColor("#5C6BC0"));
            }

            deckHolder.tvConteo.setText("M: " + deck.getConteoMain() +
                    " | E: " + deck.getConteoExtra() +
                    " | S: " + deck.getConteoSide());

            String prot = (deck.getTipoProtectores() == null || deck.getTipoProtectores().isEmpty())
                    ? "Sin protectores" : deck.getTipoProtectores() + " (" + deck.getCantidadProtectores() + ")";
            deckHolder.tvProtectores.setText(prot);

            deckHolder.itemView.setOnClickListener(v -> listener.onDeckClick(deck));
            deckHolder.itemView.setOnLongClickListener(v -> {
                listener.onDeckLongClick(deck);
                return true;
            });
        }
    }

    static class AgregarViewHolder extends RecyclerView.ViewHolder {
        public AgregarViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class DeckViewHolder extends RecyclerView.ViewHolder {
        TextView tvRegulacion, tvNombre, tvConteo, tvProtectores;

        public DeckViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRegulacion = itemView.findViewById(R.id.tvBadgeRegulacion);
            tvNombre = itemView.findViewById(R.id.tvItemNombreDeck);
            tvConteo = itemView.findViewById(R.id.tvItemConteoCartas);
            tvProtectores = itemView.findViewById(R.id.tvItemProtectores);
        }
    }
}