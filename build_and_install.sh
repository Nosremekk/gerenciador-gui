#!/bin/bash
set -e

echo "=== 1. Compilando e instalando o backend em C ==="
if [ -d "../Gerenciador-de-senhas-em-C" ]; then
    cd ../Gerenciador-de-senhas-em-C
    make clean && make
    sudo make install
    cd -
else
    echo "Pasta '../Gerenciador-de-senhas-em-C' não encontrada. Verifique se o binário 'gerenciador_senhas' já está no PATH."
fi

echo "=== 2. Compilando o pacote JavaFX (Fat JAR) ==="
mvn clean package -DskipTests

BIN_DIR="$HOME/.local/bin"
APP_DIR="$HOME/.local/share/applications"
ICON_DIR="$HOME/.local/share/icons/hicolor/128x128/apps"
SHARE_DIR="$HOME/.local/share/gerenciador-senhas"

mkdir -p "$BIN_DIR" "$APP_DIR" "$ICON_DIR" "$SHARE_DIR"

echo "=== 3. Instalando binários e assets ==="
cp target/gerenciador-gui-1.0-SNAPSHOT.jar "$SHARE_DIR/gerenciador-gui.jar"

cat << 'INNER_EOF' > "$BIN_DIR/gerenciador-gui"
#!/bin/bash
exec java -jar "$HOME/.local/share/gerenciador-senhas/gerenciador-gui.jar" "$@"
INNER_EOF
chmod +x "$BIN_DIR/gerenciador-gui"

cat << 'INNER_EOF' > "$ICON_DIR/gerenciador-senhas.svg"
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 128 128">
  <rect width="128" height="128" rx="24" fill="#1e1e2e"/>
  <rect x="28" y="52" width="72" height="52" rx="12" fill="#8aadf4"/>
  <path d="M42 52V36a22 22 0 0 1 44 0v16" fill="none" stroke="#eed49f" stroke-width="10" stroke-linecap="round"/>
  <circle cx="64" cy="74" r="6" fill="#181926"/>
  <path d="M64 78v12" stroke="#181926" stroke-width="4" stroke-linecap="round"/>
</svg>
INNER_EOF

echo "=== 4. Criando atalho no menu de aplicativos (.desktop) ==="
cat << INNER_EOF > "$APP_DIR/gerenciador-senhas.desktop"
[Desktop Entry]
Name=Cofre de Senhas (HSM)
Comment=Gerenciador de Senhas com 2FA Físico (Raspberry Pi Pico W)
Exec=$BIN_DIR/gerenciador-gui
Icon=gerenciador-senhas
Terminal=false
Type=Application
Categories=Utility;Security;
StartupNotify=true
INNER_EOF

update-desktop-database "$APP_DIR" 2>/dev/null || true

echo "=== Instalação concluída com sucesso! ==="
echo "Você já pode abrir o aplicativo buscando por 'Cofre de Senhas' no menu do sistema."
