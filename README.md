# 🌍 TripLy - Sua Rede Social de Turismo

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![Firebase](https://img.shields.io/badge/Firebase-Latest-orange.svg)](https://firebase.google.com/)
[![Android](https://img.shields.io/badge/Android-API%2033-green.svg)](https://developer.android.com/)

## 📱 Sobre o Projeto

**TripLy** é um aplicativo Android de rede social de turismo desenvolvido para o curso de Análise e Desenvolvimento de Sistemas do IFSP - Campus Araraquara. 

O app permite que viajantes compartilhem suas experiências, fotos e localizações de destinos turísticos, criando um feed colaborativo para inspirar novas aventuras.

### 🌟 Diferenciais

- 📍 **Compartilhe sua localização** - Marque a cidade onde você está viajando
- 🖼️ **Poste suas fotos** - Registre os melhores momentos da sua viagem
- 🔍 **Busque por destinos** - Encontre posts de outros viajantes por nome da cidade
- 📱 **Feed personalizado** - Veja as últimas aventuras dos viajantes

---

## 🎬 Demonstração

### Vídeo Curto (30s)
[![Clique para assistir à demonstração](https://img.youtube.com/vi/SEU_ID/0.jpg)](https://www.youtube.com/watch?v=SEU_ID)

### Vídeo Longo - Explicação do Código (5-10min)
[![Clique para assistir à explicação](https://img.youtube.com/vi/SEU_ID_LONGO/0.jpg)](https://www.youtube.com/watch?v=SEU_ID_LONGO)

---

### 🎯 Funcionalidades (Requisitos do Projeto)

| Requisito | Descrição | Status |
|-----------|-----------|--------|
| **RF1-1/2** | Login e cadastro de usuários | ✅ |
| **RF1-3** | Autenticação com Firebase Auth (email/senha) | ✅ |
| **RF1-4** | Sessão persistente (usuário permanece logado) | ✅ |
| **RF2-1** | Criar posts com imagem, texto e localização (cidade atual via GPS) | ✅ |
| **RF2-2** | Sincronização com Firebase Firestore | ✅ |
| **RF3-1** | Feed paginado (5 posts por vez) | ✅ |
| **RF3-2** | Busca de posts por nome da cidade/destino | ✅ |
| **RF3-3** | Editar perfil (nome, senha, foto) | ✅ |
| **RF4-1** | Geocodificação (coordenadas → nome da cidade) | ✅ |

---

## 🛠️ Tecnologias Utilizadas

| Categoria | Tecnologias |
|-----------|-------------|
| **Linguagem** | Kotlin |
| **Plataforma** | Android API 33 (Android 13 - Tiramisu) |
| **Backend** | Firebase Authentication + Cloud Firestore |
| **Localização** | Google Play Services (FusedLocationProviderClient + Geocoder) |
| **UI** | Material Design Components, ViewBinding, RecyclerView |
| **Imagens** | Base64 para conversão e armazenamento |

---

## 📂 Estrutura do Projeto


app/src/main/java/com/mateus/triply/
├── adapter/
│ └── PostAdapter.kt # Adapter do feed de viagens
├── auth/
│ └── UserAuth.kt # Gerenciamento de autenticação
├── dao/
│ ├── PostDao.kt # CRUD de posts (viajens)
│ └── UsuarioDao.kt # CRUD de usuários (viajantes)
├── model/
│ ├── Post.kt # Modelo da postagem (viagem)
│ └── User.kt # Modelo do usuário (viajante)
├── ui/ # Telas do app
│ ├── HomeActivity.kt # Feed de viagens
│ ├── LoginActivity.kt # Login
│ ├── SignUpActivity.kt # Cadastro de viajante
│ ├── ProfileActivity.kt # Perfil do viajante
│ ├── NewPostDialog.kt # Criar/editar post de viagem
│ └── CommentsDialog.kt # Comentários
├── utils/
│ ├── Base64Converter.kt # Conversão de imagens
│ └── LocalizacaoHelper.kt # GPS + Geocodificação
└── ...



---

## 📲 Como Executar o Projeto

### Pré-requisitos

- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 11 ou superior
- Dispositivo físico ou emulador com API 33+
- Conexão com internet

### Passo a Passo

1. **Clone o repositório**
   ```bash
   git clone https://github.com/seuusuario/triply.git
