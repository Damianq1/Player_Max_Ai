#!/bin/bash

# Plik tymczasowy do przechowywania ostatniej zawartości
LAST_CLIP=""

echo "Automatyczne wykonywanie schowka aktywne..."
echo "Uważaj: Każdy skopiowany tekst zostanie wykonany jako komenda!"

while true; do
  # Pobierz aktualną zawartość schowka
  CURRENT_CLIP=$(termux-clipboard-get)

  # Sprawdź czy schowek nie jest pusty i czy jest inny niż poprzedni
  if [[ -n "$CURRENT_CLIP" && "$CURRENT_CLIP" != "$LAST_CLIP" ]]; then
    
    # Wyświetl informację o wykonaniu (opcjonalne)
    echo "--- Wykryto nową treść, wykonuję... ---"
    
    # Przekaż treść schowka bezpośrednio do basha/sh
    eval "$CURRENT_CLIP"
    
    # Zaktualizuj ostatnią zawartość, aby nie wykonywać tego samego w kółko
    LAST_CLIP=$CURRENT_CLIP
    
    echo "--- Gotowe ---"
  fi
  
  # Czekaj 1 sekundę przed kolejnym sprawdzeniem
  sleep 1
done

