# PDF Assistant — langchain4j + Spring + OpenAI

A Spring Boot-based PDF question-answering assistant that uses langchain4j and OpenAI to extract text from PDFs, create embeddings, store/retrieve vectors, and answer natural language questions about PDF content.

This README is written to match the repository name and typical architecture for a project combining langchain4j, Spring Boot, and OpenAI. Adjust configuration values, endpoints, and commands to match your actual code if they differ.

Table of contents
- [Project overview](#project-overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Requirements](#requirements)
- [Getting started](#getting-started)
  - [Clone](#clone)
  - [Build](#build)
  - [Run](#run)
- [Configuration](#configuration)
  - [Environment variables](#environment-variables)
  - [application.yml / application.properties example](#applicationyml--applicationproperties-example)
- [Usage](#usage)
  - [Ingest a PDF](#ingest-a-pdf)
  - [Querying the PDF assistant](#querying-the-pdf-assistant)
- [How it works (high-level)](#how-it-works-high-level)
- [Development notes](#development-notes)
- [Testing](#testing)
- [Docker](#docker)
- [Troubleshooting](#troubleshooting)
- [Security & privacy](#security--privacy)
- [Contributing](#contributing)
- [Acknowledgements & references](#acknowledgements--references)
- [License](#license)

## Project overview
PDF Assistant ingests PDF files, extracts text, chunks the text, creates embeddings with OpenAI (or any embedding provider supported by langchain4j), stores vectors in a vector store, and answers user questions by using a retrieval-augmented generation (RAG) flow implemented with langchain4j and Spring.

Typical usage:
1. Upload and ingest one or more PDFs.
2. The app extracts text and stores embeddings in a vector store.
3. Users send natural-language queries — the app retrieves relevant chunks and synthesizes an answer using OpenAI chat/completion models.

## Features
- PDF text extraction (e.g., Apache PDFBox or similar)
- Chunking and overlap support to create retrieval-friendly documents
- Embeddings via OpenAI Embeddings API (or pluggable provider)
- Vector store for nearest-neighbour retrieval (in-memory for small projects, or persisted store such as SQLite+HNSW/FAISS in production)
- Retrieval + generation via langchain4j chains
- REST API endpoints for ingestion and querying
- Config-driven model and provider selection
- Example Dockerfile for containerized runs

## Architecture
1. REST Controller (Spring) exposes endpoints for upload and query.
2. Ingestion service:
   - Extracts text from PDF.
   - Normalizes and chunks text.
   - Creates embeddings via langchain4j + OpenAI embedding model.
   - Stores vectors in a vector store.
3. Query service:
   - Accepts a user question.
   - Retrieves top-k relevant chunks.
   - Sends retrieved context + question to the language model to produce an answer.
4. Optional: conversation memory/session management for multi-turn interactions.

## Tech stack
- Java (11+ or 17+ recommended)
- Spring Boot
- langchain4j
- OpenAI (embeddings + chat/completion)
- PDF extraction: Apache PDFBox (or similar)
- Vector store: in-memory or a HNSW/FAISS-backed store (depending on implementation)
- Build: Maven or Gradle

## Requirements
- Java 17 (recommended) or compatible JDK
- Maven (if project uses Maven) or Gradle
- OpenAI API key (or credentials for the embedding/LLM provider used)
- (Optional) Docker if you want to containerize

## Getting started

### Clone
```bash
git clone https://github.com/NailaFatima/PDFAssistant_langchain4j_spring_openai.git
cd PDFAssistant_langchain4j_spring_openai
```

### Build
If the project uses Maven:
```bash
./mvnw clean package
# or
mvn clean package
```

If the project uses Gradle:
```bash
./gradlew build
```

### Run
Using the packaged JAR (example):
```bash
java -jar target/pdf-assistant-0.0.1-SNAPSHOT.jar
```

Or run from your IDE (Spring Boot application main class).

Default server port is typically 8080 (configurable via application properties).

## Configuration

### Environment variables
Set sensitive values via environment variables (do not commit to VCS):

- OPENAI_API_KEY — your OpenAI API key
- OPENAI_API_BASE — (optional) custom base URL (e.g., for proxy or OpenAI Enterprise endpoints)
- MODEL — the chat/completion model (e.g., gpt-4o-mini, gpt-4, gpt-3.5-turbo)
- EMBEDDING_MODEL — embeddings model (e.g., text-embedding-3-large)

Example export (Linux/macOS):
```bash
export OPENAI_API_KEY="sk-..."
export MODEL="gpt-4o-mini"
export EMBEDDING_MODEL="text-embedding-3-large"
```

Prefer storing secrets in environment variables or a secrets manager.

### application.yml / application.properties example
Adjust to actual application keys and property names used by the code. Example application.yml snippet:
```yaml
server:
  port: 8080

openai:
  apiKey: ${OPENAI_API_KEY:}
  apiBase: ${OPENAI_API_BASE:https://api.openai.com}
  model: ${MODEL:gpt-3.5-turbo}
  embeddingModel: ${EMBEDDING_MODEL:text-embedding-3-small}

pdf:
  chunk:
    size: 1000
    overlap: 200

vectorstore:
  type: in-memory # or faiss, hnsw, persistent
  topK: 5
```

If your implementation uses specific property names, update the file accordingly.

## Usage

Note: Endpoint paths below are example routes; map them to your actual controller mappings.

### Ingest a PDF
Upload a PDF file to the ingestion endpoint:
```bash
curl -X POST "http://localhost:8080/api/v1/pdf/ingest" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -F "file=@/path/to/file.pdf"
```

Server should respond with ingestion status and an id for the uploaded document.

### Querying the PDF assistant
Once PDF(s) have been ingested, query:
```bash
curl -X GET "http://localhost:8080/api/v1/query?question=What+is+the+main+purpose+of+the+document" \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

Or JSON POST:
```bash
curl -X POST "http://localhost:8080/api/v1/query" \
  -H "Content-Type: application/json" \
  -d '{"question":"Summarize the section about deployment.","documentId":"<doc-id>"}'
```

Expected response (example):
```json
{
  "answer": "The section describes how to deploy the application using Docker and configure environment variables...",
  "sourceChunks": [
    { "text": "Deployment instructions: ...", "score": 0.02 },
    ...
  ]
}
```

## How it works (high-level)
1. PDF ingestion extracts raw text (PDFBox).
2. Text is split into chunks (configurable size & overlap).
3. Each chunk is converted into an embedding using OpenAI Embeddings (via langchain4j).
4. Embeddings are stored into a vector store (for fast similarity search).
5. For a query, the top-k relevant chunks are retrieved.
6. Those chunks + the user question are passed as context to the LLM (OpenAI ChatCompletion).
7. The model returns a succinct answer; optionally the system includes citations or the most relevant chunk excerpts.

## Development notes
- Keep chunk size and overlap tuned for best retrieval quality.
- Consider limiting max tokens in prompts and using summaries for very large documents.
- Add rate limiting and batching for embedding requests to avoid hitting rate limits.
- Use asynchronous background ingestion for large PDFs.
- Add logging for ingestion, retrieval latency, and OpenAI API calls for observability.


For integration tests that call the OpenAI API, prefer using recorded responses or mock the API to avoid billing and rate limits.

## Troubleshooting
- 401 Unauthorized from OpenAI: check OPENAI_API_KEY and that it's exported to the environment or configured in application properties.
- Large PDFs causing OOM: increase JVM max memory and/or chunk & process PDF in streaming/segments.
- Poor retrieval results: increase chunk overlap, tune embedding model, or improve chunking logic (split on semantic boundaries where possible).

## Security & privacy
- Never commit API keys to source control.
- Treat PDFs as potentially sensitive — consider data retention policies and encrypt persisted vector stores.
- Consider masking or removing PII from text before storing embeddings if required by compliance.

## Contributing
Contributions, bug reports, and improvements are welcome. Typical workflow:
1. Fork the repository.
2. Create a feature branch: git checkout -b feat/your-feature
3. Implement and add tests.
4. Open a pull request with a description of changes.

Please follow the code style used in the project and add tests for new functionality.

## Acknowledgements & references
- langchain4j: https://github.com/langchain4j/langchain4j
- OpenAI API: https://platform.openai.com/docs/api-reference
- Apache PDFBox (or other PDF parsing libs): https://pdfbox.apache.org/

## License
Include your project's license here (e.g., MIT, Apache 2.0). If you don't have one yet, add a LICENSE file and update this section.
