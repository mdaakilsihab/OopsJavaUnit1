package com.LibraryManagementDemo;

import java.io.*;
import java.util.*;

// --- Custom Exceptions ---
class BookNotAvailableException extends Exception {
    public BookNotAvailableException(String message) {
        super(message);
    }
}

class InvalidReturnException extends Exception {
    public InvalidReturnException(String message) {
        super(message);
    }
}

// --- Book Class ---
class Book {
    private String id;
    private String title;
    private boolean isIssued;

    public Book(String id, String title) {
        this.id = id;
        this.title = title;
        this.isIssued = false;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public boolean isIssued() { return isIssued; }
    public void setIssued(boolean issued) { isIssued = issued; }

    @Override
    public String toString() {
        return id + " - " + title + (isIssued ? " (Issued)" : " (Available)");
    }
}

// --- Member Class ---
class Member {
    private String memberId;
    private String name;
    private double penalty;

    public Member(String memberId, String name) {
        this.memberId = memberId;
        this.name = name;
        this.penalty = 0.0;
    }

    public String getMemberId() { return memberId; }
    public String getName() { return name; }
    public double getPenalty() { return penalty; }

    public void addPenalty(double amount) {
        penalty += amount;
    }

    @Override
    public String toString() {
        return memberId + " - " + name + " | Penalty: Rs." + penalty;
    }
}

// --- Library Class ---
class Library {
    private HashMap<String, Book> books = new HashMap<>();
    private HashMap<String, Member> members = new HashMap<>();
    private PrintWriter logWriter;

    public Library() {
        try {
            logWriter = new PrintWriter(new FileWriter("library_log.txt", true));
        } catch (IOException e) {
            System.out.println("⚠ Error creating log file!");
        }
    }

    private void log(String message) {
        if (logWriter != null) {
            logWriter.println(new Date() + " : " + message);
            logWriter.flush();
        }
    }

    // --- Add Book ---
    public void addBook(Book book) {
        books.put(book.getId(), book);
        log("Added book: " + book);
        System.out.println("✔ Book added successfully!");
    }

    // --- Add Member ---
    public void addMember(Member member) {
        members.put(member.getMemberId(), member);
        log("Added member: " + member);
        System.out.println("✔ Member added successfully!");
    }

    // --- Issue Book ---
    public void issueBook(String bookId, String memberId) throws BookNotAvailableException {
        Book book = books.get(bookId);
        Member member = members.get(memberId);

        if (book == null)
            throw new BookNotAvailableException("Book not found!");

        if (member == null)
            throw new BookNotAvailableException("Member not found!");

        if (book.isIssued())
            throw new BookNotAvailableException("Book already issued!");

        book.setIssued(true);
        log("Book issued: " + book.getTitle() + " to member " + memberId);
        System.out.println("✔ Book issued successfully!");
    }

    // --- Return Book ---
    public void returnBook(String bookId, String memberId, int daysLate)
            throws InvalidReturnException {

        Book book = books.get(bookId);
        Member member = members.get(memberId);

        if (book == null)
            throw new InvalidReturnException("Book not found!");

        if (member == null)
            throw new InvalidReturnException("Member not found!");

        if (!book.isIssued())
            throw new InvalidReturnException("Book was not issued!");

        book.setIssued(false);
        double fine = 0;

        if (daysLate > 0) {
            fine = daysLate * 2.0; // Rs. 2 per day fine
            member.addPenalty(fine);
        }

        log("Book returned: " + book.getTitle() + " by member " + memberId + ", Fine: Rs." + fine);

        System.out.println("✔ Book returned successfully!");
        if (fine > 0)
            System.out.println("⚠ Fine added: Rs." + fine);
    }

    // --- Show All Books ---
    public void showBooks() {
        System.out.println("\n--- Book List ---");
        if (books.isEmpty()) {
            System.out.println("No books found!");
            return;
        }
        books.values().forEach(System.out::println);
    }

    // --- Show All Members ---
    public void showMembers() {
        System.out.println("\n--- Member List ---");
        if (members.isEmpty()) {
            System.out.println("No members found!");
            return;
        }
        members.values().forEach(System.out::println);
    }

    public void closeLog() {
        if (logWriter != null) logWriter.close();
    }
}

// --- Main Class ---
public class LibraryManagement {

    private static int getInt(Scanner sc) {
        while (!sc.hasNextInt()) {
            System.out.println("❌ Invalid input! Enter a number:");
            sc.next();
        }
        return sc.nextInt();
    }

    public static void main(String[] args) {

        Library library = new Library();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== LIBRARY MENU ===");
            System.out.println("1. Add Book");
            System.out.println("2. Add Member");
            System.out.println("3. Issue Book");
            System.out.println("4. Return Book");
            System.out.println("5. Show All Books");
            System.out.println("6. Show All Members");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            int choice = getInt(sc);
            sc.nextLine();

            try {
                switch (choice) {
                    case 1:
                        System.out.print("Enter Book ID: ");
                        String bid = sc.nextLine();
                        System.out.print("Enter Book Title: ");
                        String title = sc.nextLine();
                        library.addBook(new Book(bid, title));
                        break;

                    case 2:
                        System.out.print("Enter Member ID: ");
                        String mid = sc.nextLine();
                        System.out.print("Enter Member Name: ");
                        String mname = sc.nextLine();
                        library.addMember(new Member(mid, mname));
                        break;

                    case 3:
                        System.out.print("Enter Book ID: ");
                        String ibid = sc.nextLine();
                        System.out.print("Enter Member ID: ");
                        String imid = sc.nextLine();
                        library.issueBook(ibid, imid);
                        break;

                    case 4:
                        System.out.print("Enter Book ID: ");
                        String rid = sc.nextLine();
                        System.out.print("Enter Member ID: ");
                        String rmid = sc.nextLine();
                        System.out.print("Enter days late: ");
                        int daysLate = getInt(sc);
                        library.returnBook(rid, rmid, daysLate);
                        break;

                    case 5:
                        library.showBooks();
                        break;

                    case 6:
                        library.showMembers();
                        break;

                    case 0:
                        System.out.println("Exiting... Goodbye!");
                        library.closeLog();
                        sc.close();
                        return;

                    default:
                        System.out.println("❌ Invalid choice! Try again.");
                }

            } catch (Exception e) {
                System.out.println("❌ Error: " + e.getMessage());
            }
        }
    }
}


