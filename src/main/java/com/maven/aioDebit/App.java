package com.maven.aioDebit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.Locale;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;
import java.util.Random;
import java.time.Instant;
import java.time.Duration;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.sql.*;

public class App extends JFrame implements ActionListener {
	
    private int switchCasePin;
    private JPanel languagePanel,cardPanel, bankPanel, initialPanel;
    private ResourceBundle messages;
    private Locale selectedLocale;
    private boolean otpVerified = false;
    private Font defaultFont, teluguFont, hindiFont;
    private boolean usePrimaryAccount = false;

    public App() {
        System.setProperty("file.encoding", "UTF-8");

        switchCasePin = loadPinFromFile();
        setTitle("Welcome to ATM Machine - Mr.Sheshadri_Ranga");
        setSize(500, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        cardPanel = new JPanel(new CardLayout());
        add(cardPanel);
        selectLanguagePanel(); // Display language panel first
        setVisible(true);  // Ensure the frame is visible
       // System.out.println("Application started.");
    }
    private void selectLanguagePanel() {
        languagePanel = new JPanel();
        languagePanel.setLayout(new GridLayout(0, 1));
        JLabel selectLanguageLabel = new JLabel("Select your language:");
        languagePanel.add(selectLanguageLabel);

        JButton englishButton = new JButton("English");
        JButton teluguButton = new JButton("తెలుగు");
        JButton hindiButton = new JButton("हिंदी");
        JButton otherButton = new JButton("Other");
        englishButton.addActionListener(e -> selectLanguage("en", "US"));
        teluguButton.addActionListener(e -> selectLanguage("te", "IN"));
        hindiButton.addActionListener(e -> selectLanguage("hi", "IN"));
        otherButton.addActionListener(e -> selectLanguage("en", "US"));

        languagePanel.add(englishButton);
        languagePanel.add(teluguButton);
        languagePanel.add(hindiButton);
        languagePanel.add(otherButton);

        cardPanel.add(languagePanel, "LanguagePanel");

        CardLayout cardLayout = (CardLayout) cardPanel.getLayout();
        cardLayout.show(cardPanel, "LanguagePanel");
        // System.out.println("Language panel displayed.");
        } 
    private void selectLanguage(String language, String country) {
       
    	selectedLocale = new Locale(language, country);
        messages = ResourceBundle.getBundle("MessagesBundle", selectedLocale);
       
        selectInitialPanel(); // Show the initial panel     
    }
    private void selectInitialPanel() {
        initialPanel = new JPanel();
        initialPanel.setLayout(new GridLayout(0, 1));
        int rollNo = 1; 
        String bankName = getBankName(rollNo);
        
        // Retrieve generic button text from the resource bundle
        String continuePrimaryTextTemplate = messages.getString("continuePrimaryAccountButton");
        String selectBankText = messages.getString("selectBankButton");

        // Format the text for the primary account button
        String continuePrimaryText = String.format(continuePrimaryTextTemplate, rollNo, bankName);

        JButton primaryAccountButton = new JButton(continuePrimaryText);
        JButton selectBankButton = new JButton(selectBankText);

        // Add action listener for the primary account button
        primaryAccountButton.addActionListener(e -> {
            usePrimaryAccount = true;
            showTransactionPanel(rollNo, bankName);
        });

        selectBankButton.addActionListener(e -> {
            usePrimaryAccount = false;
            //--------------------------------------------
            //otpVerified = User.verifyOTP();
        	boolean otpVerified = sendAsOtp();
            if (otpVerified) {
                selectBankPanel();
            } else {
                String otpFailedMessage = messages.getString("otpFailedMessage");
                showErrorDialog(otpFailedMessage);
                addResendOTPButton();
            }
        });

        initialPanel.add(primaryAccountButton);
        initialPanel.add(selectBankButton);

        cardPanel.add(initialPanel, "InitialPanel");
        CardLayout cardLayout = (CardLayout) cardPanel.getLayout();
        cardLayout.show(cardPanel, "InitialPanel");

        // System.out.println("Initial panel displayed.");
    }

    private void selectBankPanel() {
        bankPanel = new JPanel();
        bankPanel.setLayout(new GridLayout(0, 1));
        JLabel selectBankLabel = new JLabel(messages.getString("selectBank"));
        bankPanel.add(selectBankLabel);

        JButton[] bankButtons = createBankButtons();
        for (JButton bankButton : bankButtons) {
            bankPanel.add(bankButton);
            bankButton.addActionListener(this);
        }
        cardPanel.add(bankPanel, "BankPanel");
        CardLayout cardLayout = (CardLayout) cardPanel.getLayout();
        cardLayout.show(cardPanel, "BankPanel");
       // System.out.println("Bank panel displayed.");
    }

    private JButton[] createBankButtons() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/allinone", "root", "");
            Statement st = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = st.executeQuery("SELECT roll_no, bank_name FROM debitcard");

            rs.last();
            int rowCount = rs.getRow();
            rs.beforeFirst();

            JButton[] bankButtons = new JButton[rowCount];
            int i = 0;
            while (rs.next()) {
                int rln = rs.getInt("roll_no");
                String bank = rs.getString("bank_name");
                bankButtons[i] = new JButton(rln + "." + bank);
                i++;
            }
            con.close();

           // System.out.println("Bank buttons created.");
            return bankButtons;
        } catch (Exception e) {
            e.printStackTrace();
          //  System.out.println("Error creating bank buttons: " + e.getMessage());
        }
        return new JButton[0];
    }
    private String getBankName(int rollNo) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/allinone", "root", "");
            Statement st = con.createStatement();
            String query = "SELECT bank_name FROM debitcard WHERE roll_no = " + rollNo;
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {
                return rs.getString("bank_name");
            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        String selectedBank = e.getActionCommand();
        int input1 = Integer.parseInt(selectedBank.split("\\.")[0]);
        String bankName = selectedBank.split("\\.")[1]; // Get the bank name from the button text
        showTransactionPanel(input1, bankName);
    }

    private void showTransactionPanel(int rollNo, String bankName) {
        CardLayout cardLayout = (CardLayout) cardPanel.getLayout();
        cardLayout.show(cardPanel, "TransactionPanel");

        JPanel transactionPanel = new JPanel();
        transactionPanel.setLayout(new GridLayout(0, 1));

        addTransactionButtons(transactionPanel, rollNo, bankName);
        cardPanel.add(transactionPanel, "TransactionPanel");

        cardLayout.show(cardPanel, "TransactionPanel");
      //  System.out.println("Transaction panel displayed.");
    }

    private void addTransactionButtons(JPanel transactionPanel, int rollNo, String bankName) {
        JLabel selectedBankLabel = new JLabel(messages.getString("selectedBank") + rollNo + " - " + bankName);
        transactionPanel.add(selectedBankLabel);

        JLabel selectTransactionLabel = new JLabel(messages.getString("selectTransaction"));
        transactionPanel.add(selectTransactionLabel);

        JButton viewBalanceButton = new JButton(messages.getString("viewBalance"));
        JButton withdrawButton = new JButton(messages.getString("withdraw"));
        JButton depositButton = new JButton(messages.getString("deposit"));
        JButton miniStatementButton = new JButton(messages.getString("miniStatement"));
        JButton pinChangeButton = new JButton(messages.getString("changePin"));
        JButton otherServicesButton = new JButton(messages.getString("otherServices"));
        JButton exitButton = new JButton(messages.getString("exit"));

        viewBalanceButton.addActionListener(e -> performTransaction(rollNo, 1));
        withdrawButton.addActionListener(e -> performTransaction(rollNo, 2));
        depositButton.addActionListener(e -> performTransaction(rollNo, 3));
        miniStatementButton.addActionListener(e -> performTransaction(rollNo, 4));
        pinChangeButton.addActionListener(e -> performTransaction(rollNo, 5));
        otherServicesButton.addActionListener(e -> performTransaction(rollNo, 6));
        exitButton.addActionListener(e -> performTransaction(rollNo, 7));

        transactionPanel.add(viewBalanceButton);
        transactionPanel.add(withdrawButton);
        transactionPanel.add(depositButton);
        transactionPanel.add(miniStatementButton);
        transactionPanel.add(pinChangeButton);
        transactionPanel.add(otherServicesButton);
        transactionPanel.add(exitButton);
       // System.out.println("Transaction buttons added.");
    }

    private void performTransaction(int input1, int choice) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/allinone", "root", "");
            Statement st = con.createStatement();

            if (choice != 6 && choice != 7) {
                String pinInput = JOptionPane.showInputDialog(null, messages.getString("enterPin"));
                if (pinInput == null || pinInput.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null, messages.getString("pinInputError"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int enteredPin = Integer.parseInt(pinInput);
                if (enteredPin != switchCasePin) {
                    JOptionPane.showMessageDialog(null, messages.getString("incorrectPin"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
            }

            double withdrawAmount = 0;
            double depositAmount = 0;
            double amount;

            switch (choice) {
                case 1:
                    try {
                        String query1 = "SELECT deposit_balance FROM debitcard WHERE roll_no = " + input1;
                        ResultSet selectedRs1 = st.executeQuery(query1);
                        if (selectedRs1.next()) {
                            double balance = selectedRs1.getDouble("deposit_balance");
                            JOptionPane.showMessageDialog(null, messages.getString("balanceMessage") + balance);
                          //  System.out.println("Balance displayed: " + balance);
                     
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        System.out.println("Error fetching balance: " + e.getMessage());
                    }
                    break;
                case 2:
                	String withdrawAmountInput = JOptionPane.showInputDialog(null, messages.getString("enterWithdrawAmount"));
                    if (withdrawAmountInput == null || withdrawAmountInput.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(null, messages.getString("withdrawAmountError"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    withdrawAmount = Double.parseDouble(withdrawAmountInput);

                    if (withdrawAmount >= 10001) {
                    	//otpVerified = User.verifyOTP();
        	        	boolean otpVerified = sendAsOtp();
        	            if (!otpVerified) {
        	            	//otpVerified = User.verifyOTP();
                       // if (!User.verifyOTP()) {
                            JOptionPane.showMessageDialog(null, messages.getString("otpFailed"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                            break;
                        } else {
                            otpVerified = true;
                           // System.out.println("OTP is successful");
                        }
                    }

                    try {
                        String updateQuery = "UPDATE debitcard SET deposit_balance = deposit_balance - " + withdrawAmount + " WHERE roll_no = " + input1;
                        int rowsAffected = st.executeUpdate(updateQuery);
                        if (rowsAffected > 0) {
                            JOptionPane.showMessageDialog(null, messages.getString("withdrawalSuccessful"), messages.getString("success"), JOptionPane.INFORMATION_MESSAGE);

                            String withdrawalTransactionQuery = "INSERT INTO transactions (roll_no, transaction_type, amount, transaction_date) VALUES (" + input1 + ", 'Withdrawal', " + withdrawAmount + ", NOW())";
                            st.executeUpdate(withdrawalTransactionQuery);

                            String query2 = "SELECT deposit_balance FROM debitcard WHERE roll_no = " + input1;
                            ResultSet selectedRs2 = st.executeQuery(query2);

                            double remainingBalance = 0;
                            while (selectedRs2.next()) {
                                remainingBalance = selectedRs2.getDouble("deposit_balance");
                            }
                            JOptionPane.showMessageDialog(null, messages.getString("remainingBalance") + remainingBalance, messages.getString("balance"), JOptionPane.INFORMATION_MESSAGE);
                            amount = withdrawAmount;
                        } else {
                            JOptionPane.showMessageDialog(null, messages.getString("withdrawalFailed"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    break;
                case 3:
                    String depositInput = JOptionPane.showInputDialog(null, messages.getString("depositPrompt"));
                    if (depositInput != null) {
                        depositAmount = Double.parseDouble(depositInput);
                        try {
                        String query3 = "SELECT deposit_balance FROM debitcard WHERE roll_no = " + input1;
                        ResultSet selectedRs3 = st.executeQuery(query3);
                        if (selectedRs3.next()) {
                            double balance = selectedRs3.getDouble("deposit_balance");
                            balance += depositAmount;
                            String updateQuery = "UPDATE debitcard SET deposit_balance = " + balance + " WHERE roll_no = " + input1;
                            st.executeUpdate(updateQuery);
                            JOptionPane.showMessageDialog(null, messages.getString("depositSuccess") + depositAmount);
                           // System.out.println("Deposit successful: " + depositAmount);
                            String depositTransactionQuery = "INSERT INTO transactions (roll_no, transaction_type, amount, transaction_date) VALUES (" + input1 + ", 'Deposit', " + depositAmount + ", NOW())";
                            st.executeUpdate(depositTransactionQuery);
                        }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 4:
                    try {
                        // Assuming you have a table 'debitcard' which has 'roll_no' and 'bank_name'
                        String miniStatementQuery = "SELECT d.bank_name, t.transaction_id, t.transaction_type, t.amount, t.transaction_date " +
                                                    "FROM transactions t JOIN debitcard d ON t.roll_no = d.roll_no " +
                                                    "WHERE t.roll_no = " + input1;
                        
                        ResultSet miniStatementRs = st.executeQuery(miniStatementQuery);

                        StringBuilder miniStatement = new StringBuilder();

                        // Assuming the messages.getString("miniStatement") contains some header text like "Mini Statement"
                        miniStatement.append(messages.getString("miniStatement")).append("\n");

                        // Adding headers for the table
                        miniStatement.append(String.format("%-15s | %-15s | %-10s | %-20s\n", "Transaction ID", "Transaction Type", "Amount", "Date"));
                        miniStatement.append("------------------------------------------------------------\n");

                        String bankName = "";
                        while (miniStatementRs.next()) {
                            bankName = miniStatementRs.getString("bank_name");
                            int transactionId = miniStatementRs.getInt("transaction_id");
                            String transactionType = miniStatementRs.getString("transaction_type");
                            double transactionAmount = miniStatementRs.getDouble("amount");
                            String transactionDate = miniStatementRs.getString("transaction_date");
                           // double depositBalance = miniStatementRs.getDouble("deposit_balance");
                            miniStatement.append(String.format("%-15d | %-15s | %-10.2f | %-20s\n", transactionId, transactionType, transactionAmount, transactionDate));
                        }

                        // Adding bank name as a heading title
                        if (!bankName.isEmpty()) {
                            miniStatement.insert(0, "Bank Name: " + bankName + "\n\n");
                        }

                        JTextArea textArea = new JTextArea(miniStatement.toString());
                        textArea.setFont(new Font("Courier New", Font.PLAIN, 12)); // Using a monospaced font for alignment
                        textArea.setEditable(false);
                        JScrollPane scrollPane = new JScrollPane(textArea);
                        scrollPane.setPreferredSize(new Dimension(500, 400));

                        JOptionPane.showMessageDialog(null, scrollPane, messages.getString("miniStatement"), JOptionPane.INFORMATION_MESSAGE);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    break;

                case 5:
                	String newPinInput1 = JOptionPane.showInputDialog(null, messages.getString("newPinPrompt"));
                    if (newPinInput1 != null) {
                        String newPinInput2 = JOptionPane.showInputDialog(null, messages.getString("confirmPinPrompt"));
                        if (newPinInput2 != null && newPinInput1.equals(newPinInput2)) {
                            int newPin = Integer.parseInt(newPinInput1);
                            switchCasePin = newPin;
                            savePinToFile(newPin);
                            JOptionPane.showMessageDialog(null, messages.getString("pinChangeSuccess"));
                            // System.out.println("PIN changed successfully.");
                        } else {
                            JOptionPane.showMessageDialog(null, messages.getString("pinMismatchError"));
                            // System.out.println("PIN entries do not match.");
                        }
                    }
                    break;
                case 6:
                	performOtherServices(input1);
                    break;
                case 7:
                	JOptionPane.showMessageDialog(null, messages.getString("thankYou"), messages.getString("exit"), JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                    break;
            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
           // System.out.println("Error performing transaction: " + e.getMessage());
        }
    }
    
    boolean sendAsOtp() {
    	//ResourceBundle bundle = getBundle();
    	String enterOtpMessage = messages.getString("enterOtpMessage");
        String enteredOTP = JOptionPane.showInputDialog(null, enterOtpMessage);
    	//String enteredOTP = JOptionPane.showInputDialog(null, "Enter OTP:");
	    return enteredOTP != null && enteredOTP.equals("2323");
	}
    
  private void performOtherServices(int input1) {
	  // Load the PIN from a file before showing the options
	    int switchCasePin = loadPinFromFile();
	  
	    // Show the service options
	    String[] options = {
	        messages.getString("selectStatement"),
	        messages.getString("aioMiniStatement"),
	        messages.getString("pinGeneration")
	    };
	    int choice = JOptionPane.showOptionDialog(
	        null,
	        messages.getString("selectService"),
	        messages.getString("otherServices"),
	        JOptionPane.DEFAULT_OPTION,
	        JOptionPane.PLAIN_MESSAGE,
	        null,
	        options,
	        options[0]
	    );

	    switch (choice) {
	        case 0:
	        case 1:
	            // For the first two options, prompt for PIN
	            String pinInput = JOptionPane.showInputDialog(null, messages.getString("enterPin"));
	            if (pinInput == null || pinInput.trim().isEmpty()) {
	                JOptionPane.showMessageDialog(null, messages.getString("pinInputError"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
	                return;
	            }
	            int enteredPin = Integer.parseInt(pinInput);
	            if (enteredPin != switchCasePin) {
	                JOptionPane.showMessageDialog(null, messages.getString("incorrectPin"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
	                return;
	            }

	            // Proceed based on the choice
	            if (choice == 0) {
	                showCardServices();
	                JOptionPane.showMessageDialog(null, messages.getString("thankYouMessage"));
	            } else {
	                displayAllBanksMiniStatements();
	                JOptionPane.showMessageDialog(null, messages.getString("thankYouMessage"));
	            }
	          //  JOptionPane.showMessageDialog(null, "Thank you", "Information", JOptionPane.INFORMATION_MESSAGE);
	            break;
	        case 2:
	        	// Main logic block
	        	//otpVerified = App.verifyOTP();
	        	boolean otpVerified = sendAsOtp();
	        	if (otpVerified) {
	        	    // Proceed with Aadhar number input and validation
	        	    String aadharNumber = JOptionPane.showInputDialog(null, messages.getString("enterAadharNumber"));
	        	    if (aadharNumber == null || aadharNumber.trim().isEmpty()) {
	        	        JOptionPane.showMessageDialog(null, messages.getString("aadharInputError"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
	        	        return;
	        	    }

	        	    if (isAadharNumberValid(aadharNumber)) {
	        	        // Generate new PIN
	        	        String newPin = generateNewPin();
	        	        JOptionPane.showMessageDialog(null, messages.getString("newPinGenerated") + newPin);
	        	    } else {
	        	        JOptionPane.showMessageDialog(null, messages.getString("aadharNotFoundError"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
	        	    }
	        	} else {
	        	    // Handle case where OTP verification fails
	        	    String otpFailedMessage = messages.getString("otpFailedMessage");
	        	    showErrorDialog(otpFailedMessage);
	        	    addResendOTPButton();
	        	}
	        	JOptionPane.showMessageDialog(null, messages.getString("thankYouMessage"));
	        	// End of the method or block
	        	// Or if it's in a dialog or message box:
	        	
	        	//JOptionPane.showMessageDialog(null, "Thank you", "Information", JOptionPane.INFORMATION_MESSAGE);
//	        default:
//	            JOptionPane.showMessageDialog(null, messages.getString("invalidSelection"));
	            break;
	    }
	}

   
  private void showCardServices() {
	    try {
	        Class.forName("com.mysql.cj.jdbc.Driver");
	        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/allinone", "root", "");
	        Statement st = con.createStatement();

	        String bankNamesQuery = "SELECT DISTINCT bank_name FROM debitcard";
	        ResultSet bankNamesRs = st.executeQuery(bankNamesQuery);

	        java.util.List<JCheckBox> checkBoxList = new java.util.ArrayList<>();
	        JPanel checkBoxPanel = new JPanel();
	        checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

	        while (bankNamesRs.next()) {
	            String bankName = bankNamesRs.getString("bank_name");
	            JCheckBox checkBox = new JCheckBox(bankName);
	            checkBoxList.add(checkBox);
	            checkBoxPanel.add(checkBox);
	        }

	        JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
	        scrollPane.setPreferredSize(new Dimension(300, 200));

	        int result = JOptionPane.showConfirmDialog(null, scrollPane, messages.getString("selectBank"), JOptionPane.OK_CANCEL_OPTION);

	        if (result == JOptionPane.OK_OPTION) {
	            java.util.List<String> selectedBanks = new java.util.ArrayList<>();
	            for (JCheckBox checkBox : checkBoxList) {
	                if (checkBox.isSelected()) {
	                    selectedBanks.add(checkBox.getText());
	                }
	            }
	            displayMiniStatementsForSelectedBanks(selectedBanks);
	        }

	        con.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	private void displayMiniStatementsForSelectedBanks(java.util.List<String> selectedBanks) {
	    try {
	        Class.forName("com.mysql.cj.jdbc.Driver");
	        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/allinone", "root", "");
	        Statement st = con.createStatement();

	        StringBuilder allBanksMiniStatements = new StringBuilder();

	        for (String bankName : selectedBanks) {
	            String miniStatementQuery = "SELECT d.bank_name, t.transaction_id, t.transaction_type, t.amount, t.transaction_date " +
	                                        "FROM transactions t JOIN debitcard d ON t.roll_no = d.roll_no " +
	                                        "WHERE d.bank_name = '" + bankName + "' " +
	                                        "ORDER BY t.transaction_date";
	            ResultSet miniStatementRs = st.executeQuery(miniStatementQuery);

	            allBanksMiniStatements.append("Bank Name: ").append(bankName).append("\n");
	            allBanksMiniStatements.append(String.format("%-15s | %-15s | %-10s | %-20s\n", "Transaction ID", "Transaction Type", "Amount", "Date"));
	            allBanksMiniStatements.append("------------------------------------------------------------\n");

	            while (miniStatementRs.next()) {
	                int transactionId = miniStatementRs.getInt("transaction_id");
	                String transactionType = miniStatementRs.getString("transaction_type");
	                double transactionAmount = miniStatementRs.getDouble("amount");
	                String transactionDate = miniStatementRs.getString("transaction_date");

	                allBanksMiniStatements.append(String.format("%-15d | %-15s | %-10.2f | %-20s\n", transactionId, transactionType, transactionAmount, transactionDate));
	            }

	            allBanksMiniStatements.append("\n\n");
	        }

	        JTextArea textArea = new JTextArea(allBanksMiniStatements.toString());
	        textArea.setFont(new Font("Courier New", Font.PLAIN, 12));
	        textArea.setEditable(false);
	        JScrollPane scrollPane = new JScrollPane(textArea);
	        scrollPane.setPreferredSize(new Dimension(600, 400));

	        JOptionPane.showMessageDialog(null, scrollPane, messages.getString("allBanksMiniStatements"), JOptionPane.INFORMATION_MESSAGE);
	        con.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}


  
   private void displayAllBanksMiniStatements() {
      try {
          Class.forName("com.mysql.cj.jdbc.Driver");
          Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/allinone", "root", "");
          Statement st = con.createStatement();

          String allBanksMiniStatementQuery = "SELECT d.bank_name, t.transaction_id, t.transaction_type, t.amount, t.transaction_date FROM transactions t JOIN debitcard d ON t.roll_no = d.roll_no ORDER BY d.bank_name, t.transaction_date";
          ResultSet allBanksMiniStatementRs = st.executeQuery(allBanksMiniStatementQuery);

          StringBuilder allBanksMiniStatement = new StringBuilder();
          allBanksMiniStatement.append(messages.getString("allBanksMiniStatements")).append("\n");

          String currentBank = "";
          while (allBanksMiniStatementRs.next()) {
              String bankName = allBanksMiniStatementRs.getString("bank_name");
              int transactionId = allBanksMiniStatementRs.getInt("transaction_id");
              String transactionType = allBanksMiniStatementRs.getString("transaction_type");
              double transactionAmount = allBanksMiniStatementRs.getDouble("amount");
              String transactionDate = allBanksMiniStatementRs.getString("transaction_date");

              if (!bankName.equals(currentBank)) {
                  if (!currentBank.isEmpty()) {
                      allBanksMiniStatement.append("\n");
                  }
                  currentBank = bankName;
                  allBanksMiniStatement.append("\n").append(currentBank).append(" Transactions:\n");
                  allBanksMiniStatement.append(String.format("%-15s | %-20s | %-15s | %-10s\n", "Transaction ID", "Transaction Type", "Amount", "Date"));
                  allBanksMiniStatement.append("------------------------------------------------------------\n");
              }

              allBanksMiniStatement.append(String.format("%-15d | %-20s | %-15.2f | %-10s\n", transactionId, transactionType, transactionAmount, transactionDate));
          }

          JTextArea textArea = new JTextArea(allBanksMiniStatement.toString());
          textArea.setFont(new Font("Courier New", Font.PLAIN, 12)); // Use Courier New or another monospaced font
          textArea.setEditable(false);
          JScrollPane scrollPane = new JScrollPane(textArea);
          scrollPane.setPreferredSize(new Dimension(600, 400));

          JOptionPane.showMessageDialog(null, scrollPane, messages.getString("allBanksMiniStatements"), JOptionPane.INFORMATION_MESSAGE);
          con.close();
      } catch (Exception e) {
          e.printStackTrace();
      }
  }
//--------------------
   public boolean isAadharNumberValid(String aadharNumber) {
	    // For example, you might query a database or perform some validation
	    return aadharNumber != null && !aadharNumber.trim().isEmpty() && aadharNumber.matches("\\d{12}");
	}

//Example of checking Aadhar number validity and generating PIN
 private String generatePinIfAadharValid(String aadharNumber) {
     // Validate Aadhar number
     if (aadharNumber == null || aadharNumber.isEmpty()) {
         System.out.println("Invalid Aadhar number.");
         return null;
     }

     // Check Aadhar number validity in the database
     String query = "SELECT COUNT(*) FROM debitcard WHERE aadhar_no = ?";
     try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/allinone", "root", "");
          PreparedStatement pstmt = conn.prepareStatement(query)) {
         pstmt.setString(1, aadharNumber);
         try (ResultSet rs = pstmt.executeQuery()) {
             if (rs.next() && rs.getInt(1) > 0) {
                 // Aadhar number exists in the database, generate a new PIN
                 String newPin = generateNewPin();
                 if (newPin != null) {
                     return newPin;
                 }
             } else {
                 System.out.println("Invalid Aadhar number.");
             }
         }
     } catch (SQLException e) {
         System.out.println("Error checking Aadhar number validity or generating PIN.");
         e.printStackTrace(); // Consider logging instead in a production environment
     }
     return null;
 }

 // Helper method to generate a new 4-digit PIN
 private String generateNewPin() {
     Random random = new Random();
     int pin = random.nextInt(9000) + 1000; // Generates a random 4-digit number
     return String.valueOf(pin);
     
 }
 
     private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(null, message, messages.getString("error"), JOptionPane.ERROR_MESSAGE);
    }

    private int loadPinFromFile() {
        try {					
        	 File file = new File("\pin");
          //  File file = new File("C:\\Users\\Admin\\Desktop\\AioWebPage\\pin.txt");//C:\Users\Admin\Desktop\aio
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                if (scanner.hasNextInt()) {
                    return scanner.nextInt();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
           // System.out.println("Error loading PIN from file: " + e.getMessage());
        }
        return 0;
    }

    private void savePinToFile(int pin) {
        try (FileWriter writer = new FileWriter("\pin")) {
            writer.write(String.valueOf(pin));
           // System.out.println("PIN saved to file.");
        } catch (IOException e) {
            e.printStackTrace();
          //  System.out.println("Error saving PIN to file: " + e.getMessage());
        }
    }

    private boolean verifyOTP() {
        String generatedOTP = generateOTP();
        sendOTPToUser(generatedOTP);

        String userInput = JOptionPane.showInputDialog(null, "Enter the OTP sent to your registered mobile number:");
        if (userInput == null || userInput.trim().isEmpty()) {
            return false;
        }

        boolean isOTPValid = userInput.equals(generatedOTP);
     // boolean isOTPValid = userInput.equals(generatedOTP);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                JOptionPane.showMessageDialog(null, "OTP verification time has expired!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }, 90000); // 1 minute 30 seconds

        return isOTPValid;
    }
    private void sendOTPToUser(String otp) {
        // Simulate sending OTP to user's registered mobile number
        JOptionPane.showMessageDialog(null, "OTP sent to your registered mobile number: " + otp, "OTP Sent", JOptionPane.INFORMATION_MESSAGE);
    }
    private String generateOTP() {
        return String.format("%06d", (int) (Math.random() * 1000000));
   }
    private void addResendOTPButton() {
        JButton resendOTPButton = new JButton("Resend OTP");
        resendOTPButton.addActionListener(e -> {
        	//----------------------------------------
           // otpVerified = App.verifyOTP();
        	boolean otpVerified = sendAsOtp();
            if (otpVerified) {
                selectBankPanel();
            } else {
                showErrorDialog("OTP verification failed! Click 'Resend OTP' to try again.");
            }
        });
        initialPanel.add(resendOTPButton);
        initialPanel.revalidate();
        initialPanel.repaint();
    }
//------------------
 

    public static class User extends JFrame {
        public static final String ACCOUNT_SID = "AC8754a59790bf1a0b91982da3307229ce";
        public static final String AUTH_TOKEN = "98590bc6804882a30d68a763db198983";

        // Database credentials
        public static final String DB_URL = "jdbc:mysql://localhost/debit_card";
        public static final String DB_USER = "root";
        public static final String DB_PASSWORD = "";

        public void VerifyOtp() {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            String mobileNumber = retrieveMobileNumberFromDatabase();
            if (mobileNumber != null) {
                String otp = generateOTP();
                Instant otpGenerationTime = Instant.now(); // Record the time when the OTP is generated

                Message message = Message.creator(
                        new PhoneNumber("+91" + mobileNumber),
                        new PhoneNumber("+12513158548"),
                        "Your AIO card was swiped your alert OTP is : " + otp)
                        .create();

                System.out.println("Message SID: " + message.getSid());
                String enteredOTP = JOptionPane.showInputDialog("Enter the OTP received on your mobile:");

                // Check if the entered OTP is within the 1 minute 30 seconds window
                if (enteredOTP != null) {
                    Instant currentTime = Instant.now();
                    Duration duration = Duration.between(otpGenerationTime, currentTime);
                    if (duration.getSeconds() < 90) { // 90 seconds = 1 minute 30 seconds
                        if (enteredOTP.equals(otp)) {
                            JOptionPane.showMessageDialog(null, "OTP is correct!");
                           //--------------------------check once
                            User card = new User();
                            card.setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(null, "OTP is incorrect!");
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "OTP is expired!");
                    }
                }
            } else {
                System.out.println("Failed to retrieve mobile number from the database.");
            }
        }

        private static String generateOTP() {
            Random random = new Random();
            int otp = 10000 + random.nextInt(90000); // Generate a number between 10000 and 99999
            return String.valueOf(otp);
        }

        private static String retrieveMobileNumberFromDatabase() {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            String mobileNumber = null;

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/allinone", "root", "");
                String query = "SELECT mobile_no FROM debitcard";
                stmt = conn.prepareStatement(query);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    mobileNumber = rs.getString("mobile_no");
                }
            } catch (SQLException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (stmt != null) stmt.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            return mobileNumber;
        }

        public static boolean verifyOTP() {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            String mobileNumber = retrieveMobileNumberFromDatabase();
            if (mobileNumber != null) {
                String otp = generateOTP();
                Instant otpGenerationTime = Instant.now();

                Message message = Message.creator(
                        new PhoneNumber("+91" + mobileNumber),
                        new PhoneNumber("+12513158548"),
                        "Your AIO card OTP is: " + otp)
                        .create();

                System.out.println("Message SID: " + message.getSid());

                String enteredOTP = JOptionPane.showInputDialog("Enter the OTP received on your mobile:");
                if (enteredOTP != null) {
                    Instant currentTime = Instant.now();
                    Duration duration = Duration.between(otpGenerationTime, currentTime);
                    return duration.getSeconds() < 90 && enteredOTP.equals(otp); // 90 seconds = 1 minute 30 seconds
                } else {
                    System.out.println("Failed to retrieve mobile number from the database.");
                    return false;
                }
            }
            return false;
        }
    }


   //---------------------------- 
    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::new);
        
    }
}
