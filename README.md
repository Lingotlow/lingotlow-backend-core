# Lingotlow Backend Core

## Overview

Este projeto backend-core do Lingotlow usa Java 21 + Spring Boot e depende de serviços externos para rodar localmente, que levantamos com Docker Compose.

## Stack das Dependências Levantadas

- PostgreSQL 16
- Redis 7
- PgAdmin 4 (interface para administrar o PostgreSQL)
- Rede Docker personalizada (lingotlow-network)

O backend Spring Boot será executado localmente via IntelliJ.

---

## Pré-requisitos para Mac e Linux (LMDE 6 - Debian)

### 1. Instalar Docker e Docker Compose

### No Linux LMDE 6 (Debian base):
##### Atualizar os pacotes
    sudo apt update && sudo apt upgrade -y

##### Instalar pacotes necessários para adicionar repositórios
    sudo apt install -y ca-certificates curl gnupg lsb-release

##### Criar chave para o repositório oficial Docker
    sudo mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

##### Adicionar o repositório Docker no sources.list
    echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian \
    $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

##### Atualizar lista de pacotes e instalar Docker Engine e Docker Compose plugin
    sudo apt update
    sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

##### Verificar instalação
    docker --version
    docker compose version

##### Iniciar o serviço Docker (se não estiver rodando)
    sudo systemctl start docker
    sudo systemctl enable docker

##### Adicionar seu usuário ao grupo docker para não precisar usar sudo
    sudo usermod -aG docker $USER

##### Após isso, faça logout/login ou reinicie o sistema para aplicar

### No macOS:

#### Instale o Docker Desktop para Mac via https://docs.docker.com/desktop/install/mac-install/
#### Siga o instalador e inicie o Docker Desktop

#### Verifique no terminal:
    docker --version
    docker compose version

#### Como Rodar o Docker Compose
#### No diretório do projeto, onde está o arquivo docker-compose.yaml, execute:
    docker compose up -d

#### Isso vai criar e iniciar os containers de:

- PostgreSQL
- Redis
- PgAdmin
- Rede docker "lingotlow-network"

## Como Testar os Serviços

### Se não tiver instalado redis-cli:
#### No Linux Debian/LMDE:
    sudo apt install redis-tools

#### No macOS (se usar Homebrew):
    brew install redis

#### Teste:
    redis-cli -h localhost ping
    # Deve responder PONG

### Se não tiver instalado psql:
#### No Linux Debian/LMDE:
    sudo apt install postgresql-client

#### No macOS:
    brew install libpq
    brew link --force libpq

#### Teste conexão:
    psql -h localhost -p 5432 -U lingotlow -d lingotlow_dev
    # Senha configurada no docker-compose.yaml

### Acessar PgAdmin:
    Acesse pelo navegador http://localhost:5050
    Use o e-mail e senha configurados nas variáveis de ambiente no docker-compose.yaml (exemplo: admin@example.com)
    Configure uma nova conexão para o banco PostgreSQL usando host postgres, usuário e senha conforme docker-compose.yaml

### Caso precise parar os containers:
    docker compose down

### Se fizer alterações no docker-compose.yaml, rode:
    docker compose up -d --build

### Verifique logs com:
    docker compose logs -f

### Próximos passos
    Rodar o backend core no IntelliJ
    Configurar application.yaml para conectar no PostgreSQL e Redis locais

### Em caso de dúvidas, consulte a documentação do Docker:
#### Docker Compose: https://docs.docker.com/compose/
#### Docker para Debian: https://docs.docker.com/engine/install/debian/
#### Docker para macOS: https://docs.docker.com/docker-for-mac/

#### Obrigado por usar o Lingotlow!