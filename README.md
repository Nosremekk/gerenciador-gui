# Cofre de Senhas — Interface Gráfica (JavaFX) com 2FA em Hardware

Interface gráfica moderna desenvolvida em **JavaFX** para o ecossistema de gerenciamento seguro de credenciais, integrada ao backend nativo em C e com autenticação física de dois fatores (**2FA / HSM**) via **Raspberry Pi Pico W**.

---

## 🚀 Funcionalidades

### 🔐 Segurança & Hardware 2FA
- **Autenticação Física Obrigatória**: Acesso e operações críticas (adição, edição, exclusão e troca de chave) exigem a presença da chave física na porta serial (`/dev/ttyACM0`) e confirmação manual no botão do Raspberry Pi Pico W.
- **Monitoramento em Tempo Real**: Detecção contínua do estado de conexão da chave de hardware.
- **Auto-Lock por Desconexão**: Bloqueio instantâneo do cofre caso o token físico seja removido durante o uso.
- **Auto-Lock por Inatividade**: Bloqueio automático de segurança após 120 segundos sem interação do usuário na interface.
- **Clipboard Seguro**: Cópia de senhas com contagem regressiva e limpeza automática da área de transferência após 30 segundos.

### 🎨 Interface & Experiência de Uso
- **Temas Dinâmicos (Modo Claro & Modo Escuro)**:
  - **Modo Escuro**: Baseado na paleta *Catppuccin Mocha*.
  - **Modo Claro**: Baseado na paleta *Catppuccin Latte*.
  - Alternância instantânea em tempo real (sem necessidade de reiniciar o aplicativo) e persistência automática das preferências do usuário.
- **Visualização Flexível**: Botão de alternância de visibilidade das senhas (olhinho) tanto na tabela geral quanto nos formulários de cadastro e edição.
- **Busca em Tempo Real**: Filtragem instantânea por nome do serviço ou usuário.
- **Atalhos e Menu de Contexto**: Duplo clique na linha para editar credenciais e suporte a menu de clique direito (Copiar, Editar, Excluir).

### 🛠️ Gerenciamento de Credenciais
- **Cadastro e Edição Completa**: Armazenamento seguro de serviço, usuário e senha.
- **Gerador de Senhas Integrado**: Criação de senhas aleatórias fortes de 8 a 64 caracteres, com visualização imediata da senha gerada.
- **Auditoria de Segurança**: Painel visual com índice percentual de robustez do cofre, contagem de senhas por nível de força (fracas, médias, fortes, muito fortes) e alerta de credenciais duplicadas.
- **Backup Criptografado & Exportação CSV**: Criação de cópias de segurança do banco e exportação auditada para formato CSV.
- **Troca de Senha Mestra**: Re-derivação de chaves criptográficas em conjunto com o token de hardware.

---

## 🏗️ Arquitetura do Projeto

```text
gerenciador-gui/
├── build_and_install.sh        # Script automatizado de compilação e instalação
├── pom.xml                     # Configuração de build Maven e plugins JavaFX/Shade
├── src/
│   └── main/
│       ├── java/com/cofre/
│       │   ├── Launcher.java               # Classe de entrada para inicialização do Fat JAR
│       │   ├── MainApp.java                # Tela de autenticação e desbloqueio
│       │   ├── DashboardView.java          # Painel principal de credenciais e modais
│       │   ├── ThemeManager.java           # Gerenciamento dinâmico e persistência de temas
│       │   ├── model/
│       │   │   ├── Credencial.java         # Modelo de dados observável da credencial
│       │   │   └── ResultadoAuditoria.java # Modelo dos resultados da análise de segurança
│       │   └── service/
│       │       ├── BackendService.java     # Ponte de execução com o binário C (stdin/stdout)
│       │       └── HardwareMonitor.java    # Thread de monitoramento dinâmico do dispositivo USB
│       └── resources/
│           ├── dark-theme.css              # Estilos do tema escuro
│           ├── light-theme.css             # Estilos do tema claro
│           └── styles.css                  # Folha de estilo base
└── .gitignore                  # Regras de exclusão do controle de versão Git
```

---

## 📋 Pré-requisitos

- **Java JDK 21** ou superior
- **Apache Maven 3.8+**
- **Binário do Backend**: `gerenciador_senhas` compilado e acessível em `~/.local/bin/gerenciador_senhas`
- **Raspberry Pi Pico W**: Gravado com o firmware do token 2FA e conectado via USB (`/dev/ttyACM0`)

---

## ⚙️ Compilação e Instalação

### Instalação Automática
O script `build_and_install.sh` compila o pacote JavaFX, gera o executável no PATH do usuário e cria o atalho no menu de aplicativos do sistema:

```bash
chmod +x build_and_install.sh
./build_and_install.sh
```

### Compilação Manual via Maven
Para compilar o pacote com todas as dependências embutidas (Fat JAR):

```bash
mvn clean package -DskipTests
```

O arquivo executável será gerado em:
```text
target/gerenciador-gui-1.0-SNAPSHOT.jar
```

Para executar diretamente:
```bash
java -jar target/gerenciador-gui-1.0-SNAPSHOT.jar
```

---

## 📄 Licença

Este projeto é distribuído sob a licença [MIT](LICENSE).

