# Student Data Client-Server System

A multithreaded client-server application built in Java (CMPE412 — Systems Programming). The system serves student grade data across a network, with a full Swing GUI for both server and client sides.

---

## Overview

The server hosts five course files containing student records. Clients connect over TCP, browse available files, and retrieve student data or grade statistics — all through a graphical interface.

The architecture cleanly separates business logic from the UI layer, making it easy to run either a console version (Phase I) or the GUI version (Phase II) on top of the same core.

---

## Architecture

```
ServerCore.java       ← Business logic: sockets, file I/O, thread pool
     ↑
ServerGUI.java        ← Swing GUI wrapping ServerCore

ClientGUI.java        ← GUI client following the Phase I protocol
```

**Phase I** (console) and **Phase II** (GUI) share the same network protocol — the GUI is purely additive.

---

## Features

### Server
- Starts a TCP server on a configurable port (default `8089`)
- Auto-detects and displays the machine's local IP for easy client setup
- Uses a **thread pool (5 threads)** to handle concurrent clients
- Runs two background tasks on startup:
  - **MergeFilesTask** — reads all 5 course files, deduplicates students (keeping best grade), writes `AllStudents.txt`
  - **AnalysisTask** — computes average, best, and lowest grade, writes `Overview.txt`
- Real-time activity log with timestamps in the GUI

### Client
- Connects to server by IP and port
- Dynamically renders a button for each available course file
- Displays student records in a formatted table (Name, Student ID, Grade + letter grade)
- Shows an overview statistics panel (total students, average, best, lowest grade)
- Disconnect and reconnect without restarting

---

## How to run

### 1. Compile

```bash
javac ServerCore.java ServerGUI.java
javac ClientGUI.java
```

### 2. Set up course data files

Create five `.txt` files in the same directory as the server, each with 30+ records in the format:

```
Student Name, StudentID, Grade
Alice Johnson, 2023001, 95
Bob Williams, 2023002, 87
...
```

Files: `CS101.txt`, `MATH201.txt`, `CMPE351.txt`, `CMPE411.txt`, `CMPE431.txt`

### 3. Start the server

```bash
java ServerGUI
```

Click **Start Server**. Note the IP address shown — clients will need it.

### 4. Start the client

```bash
java ClientGUI
```

Enter the server IP (or `localhost` for same machine) and click **Connect**.

---

## Network protocol

```
Server → Client:  FILELIST
                  CS101.txt
                  MATH201.txt
                  ...
                  OVERVIEW
                  END

Client → Server:  CS101.txt       (or OVERVIEW)

Server → Client:  CONTENT
                  Alice Johnson, 2023001, 95
                  ...
                  END
```

---

## What was used

- Java Sockets (`ServerSocket`, `Socket`)
- `ExecutorService` / thread pool (`Executors.newFixedThreadPool`)
- Java Swing (`JFrame`, `JTable`, `JTextArea`, `JScrollPane`, `SwingUtilities.invokeLater`)
- File I/O (`BufferedReader`, `BufferedWriter`)
- Layout managers: `BorderLayout`, `GridLayout`, `FlowLayout`

---

## Concepts demonstrated

- Client-server architecture over raw TCP
- Multithreaded server with thread pool
- Thread-safe GUI updates via `SwingUtilities.invokeLater()`
- Separation of concerns: business logic (`ServerCore`) vs. presentation (`ServerGUI`)
- File merging and deduplication with a `HashMap`
- Background task scheduling with `ExecutorService`
