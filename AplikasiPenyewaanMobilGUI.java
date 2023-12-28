import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class Mobil {
    private String nama;
    private int tahunProduksi;
    private double hargaSewaPerHari;
    private boolean tersedia;
    private String kategori;

    public Mobil(String nama, int tahunProduksi, double hargaSewaPerHari, String kategori) {
        this.nama = nama;
        this.tahunProduksi = tahunProduksi;
        this.hargaSewaPerHari = hargaSewaPerHari;
        this.tersedia = true;
        this.kategori = kategori;
    }

    public String getNama() {
        return nama;
    }

    public int getTahunProduksi() {
        return tahunProduksi;
    }

    public double getHargaSewaPerHari() {
        return hargaSewaPerHari;
    }

    public String getKategori() {
        return kategori;
    }

    public void setKategori(String kategori) {
        this.kategori = kategori;
    }

    public boolean isTersedia() {
        return tersedia;
    }

    public void setTersedia(boolean tersedia) {
        this.tersedia = tersedia;
    }
}

interface RentalApi {
    boolean checkAvailability(String namaMobil);
}

class DummyRentalApi implements RentalApi {
    @Override
    public boolean checkAvailability(String namaMobil) {
        return true;
    }
}

class RentalRecord {
    private static int nextTransactionId = 1;

    private int transactionId;
    private Mobil mobil;
    private int lamaSewa;
    private double totalBiaya;

    public RentalRecord(Mobil mobil, int lamaSewa, double totalBiaya) {
        this.transactionId = nextTransactionId++;
        this.mobil = mobil;
        this.lamaSewa = lamaSewa;
        this.totalBiaya = totalBiaya;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public Mobil getMobil() {
        return mobil;
    }

    public int getLamaSewa() {
        return lamaSewa;
    }

    public void setLamaSewa(int lamaSewa) {
        this.lamaSewa = lamaSewa;
        this.totalBiaya = mobil.getHargaSewaPerHari() * lamaSewa;
    }

    public double getTotalBiaya() {
        return totalBiaya;
    }
}

class PenyewaanMobil {
    private List<RentalRecord> rentalRecords;
    private RentalApi rentalApi;
    private Mobil[] mobil;
    private DefaultTableModel tableModel;
    private JTable table;

    public PenyewaanMobil(Mobil[] mobil, RentalApi rentalApi) {
        this.rentalRecords = new ArrayList<>();
        this.rentalApi = rentalApi;
        this.mobil = mobil;
    }

    public void sewaMobil(String namaMobil, int lamaSewa) {
        if (lamaSewa < 1 || lamaSewa > 7) {
            JOptionPane.showMessageDialog(null, "Lama sewa harus antara 1 dan 7 hari.");
            return;
        }

        if (rentalApi.checkAvailability(namaMobil)) {
            for (Mobil m : mobil) {
                if (m.getNama().equals(namaMobil) && m.isTersedia()) {
                    double totalBiaya = m.getHargaSewaPerHari() * lamaSewa;
                    String receipt = generateReceipt(m, lamaSewa, totalBiaya);
                    saveReceiptToFile(receipt);
                    m.setTersedia(false);

                    rentalRecords.add(new RentalRecord(m, lamaSewa, totalBiaya));

                    return;
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Maaf, mobil tidak tersedia atau nama mobil tidak valid.");
        }
    }

    public void tampilkanDanKelolaStruk() {
        String struk = ambilStrukDariFile();
        if (struk != null && !struk.isEmpty()) {
            tampilkanStrukTabel();
        } else {
            JOptionPane.showMessageDialog(null, "Tidak ada struk yang dapat ditampilkan.");
        }
    }


    public void tampilkanMobil(DefaultTableModel tableModel) {
        for (Mobil m : mobil) {
            tableModel.addRow(new Object[]{m.getNama(), m.getTahunProduksi(), m.getHargaSewaPerHari(), m.isTersedia(), m.getKategori()});
        }
    }

    private void saveRentalRecordsToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("rental_records.dat"))) {
            oos.writeObject(rentalRecords);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Gagal menyimpan catatan penyewaan ke file.");
            e.printStackTrace();
        }
    }

    private void tampilkanStrukTabel() {
        JFrame frame = new JFrame("Daftar Struk Penyewaan");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        tableModel = new DefaultTableModel(
                new String[]{"No. Transaksi", "Nama Mobil", "Tahun Produksi", "Lama Sewa", "Total Biaya"},
                0);

        for (RentalRecord record : rentalRecords) {
            tableModel.addRow(new String[]{
                    String.valueOf(record.getTransactionId()),
                    record.getMobil().getNama(),
                    String.valueOf(record.getMobil().getTahunProduksi()),
                    String.valueOf(record.getLamaSewa()),
                    String.valueOf(record.getTotalBiaya())
            });
        }

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(table);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        JButton updateButton = new JButton("Update Struk");
        updateButton.addActionListener(e -> updateStruk());
        JButton hapusButton = new JButton("Hapus Struk");
        hapusButton.addActionListener(e -> hapusStruk());
        JButton tutupButton = new JButton("Tutup");
        tutupButton.addActionListener(e -> frame.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(updateButton);
        buttonPanel.add(hapusButton);
        buttonPanel.add(tutupButton);

        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void updateStruk() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int selectedRecordIndex = selectedRow;
            RentalRecord selectedRecord = rentalRecords.get(selectedRecordIndex);

            String newLamaSewaStr = JOptionPane.showInputDialog("Masukkan lama sewa (hari):");
            if (newLamaSewaStr != null) {
                try {
                    int newLamaSewa = Integer.parseInt(newLamaSewaStr);
                    selectedRecord.setLamaSewa(newLamaSewa);

                    double newTotalBiaya = selectedRecord.getMobil().getHargaSewaPerHari() * newLamaSewa;
                    String updatedReceipt = generateReceipt(selectedRecord.getMobil(), newLamaSewa, newTotalBiaya);

                    // Update struk dalam file
                    updateReceiptInFile(updatedReceipt, selectedRecordIndex);

                    // Update tampilan tabel
                    tableModel.setValueAt(newLamaSewaStr, selectedRow, 3);
                    tableModel.setValueAt(String.valueOf(newTotalBiaya), selectedRow, 4);

                    JOptionPane.showMessageDialog(null, "Struk berhasil diupdate.");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Masukkan lama sewa yang valid (angka).");
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Pilih struk terlebih dahulu.");
        }
    }

    private void hapusStruk() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            int confirm = JOptionPane.showConfirmDialog(null, "Apakah Anda yakin ingin menghapus struk?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                int selectedRecordIndex = selectedRow;
                rentalRecords.remove(selectedRecordIndex);
                saveRentalRecordsToFile();

                // Hapus baris dari tampilan tabel
                tableModel.removeRow(selectedRow);

                JOptionPane.showMessageDialog(null, "Struk berhasil dihapus.");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Pilih struk terlebih dahulu.");
        }
    }

    private void updateReceiptInFile(String updatedStruk, int recordIndex) {
        try (RandomAccessFile file = new RandomAccessFile("rental_records.txt", "rw")) {
            file.seek(0);

            for (int i = 0; i < recordIndex; i++) {
                // Move the file pointer to the correct position
                file.readLine();
            }

            file.writeBytes(updatedStruk + System.lineSeparator());

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Gagal menyimpan struk ke file.");
            e.printStackTrace();
        }
    }

    private String generateReceipt(Mobil mobil, int lamaSewa, double totalBiaya) {
        // Implementasi metode ini sesuai dengan format struk yang diinginkan
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = dateFormat.format(new Date());

        return String.format("Tanggal: %s\nNama Mobil: %s\nLama Sewa: %d hari\nTotal Biaya: %.2f\n",
                dateStr, mobil.getNama(), lamaSewa, totalBiaya);
    }

    private void saveReceiptToFile(String receipt) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("rental_records.txt", true))) {
            writer.write(receipt);
            writer.newLine();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Gagal menyimpan struk ke file.");
            e.printStackTrace();
        }
    }

    private String ambilStrukDariFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("rental_records.txt"))) {
            StringBuilder struk = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                struk.append(line).append("\n");
            }

            return struk.toString();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Gagal membaca struk dari file.");
            e.printStackTrace();
            return null;
        }
    }
}

public class AplikasiPenyewaanMobilGUI {
    private PenyewaanMobil penyewaan;

    public AplikasiPenyewaanMobilGUI() {
        Mobil mobil1 = new Mobil("Avanza", 2020, 50.0, "Mobil Pribadi");
        Mobil mobil2 = new Mobil("Innova", 2021, 70.0, "Mobil Pribadi");
        Mobil mobil3 = new Mobil("Ertiga", 2019, 60.0, "Mobil Pribadi");
        Mobil mobil4 = new Mobil("Brio RS", 2021, 60.0, "Mobil Pribadi");
        Mobil mobil5 = new Mobil("Hilux", 2019, 80.0, "Pickup");
        Mobil mobil6 = new Mobil("Ford Raptor", 2022, 60.0, "Pickup");
        Mobil mobil7 = new Mobil("Hilux", 2018, 80.0, "Box");
        Mobil mobil8 = new Mobil("Elf", 2020, 60.0, "Mini Bus");

        Mobil[] daftarMobil = {mobil1, mobil2, mobil3, mobil4, mobil5, mobil6, mobil7, mobil8};


        RentalApi rentalApi = new DummyRentalApi();

        SwingUtilities.invokeLater(() -> createAndShowGUI(daftarMobil, rentalApi));
    }

    private void createAndShowGUI(Mobil[] daftarMobil, RentalApi rentalApi) {
        JFrame frame = new JFrame("Aplikasi Penyewaan Mobil");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1));

        JButton tampilkanButton = new JButton("Tampilkan Daftar Mobil");
        JButton sewaButton = new JButton("Sewa Mobil");
        JButton tampilkanStrukButton = new JButton("Tampilkan Struk");
        JButton keluarButton = new JButton("Keluar");

        DefaultTableModel tableModel = new DefaultTableModel(
                new String[]{"Nama", "Tahun Produksi", "Harga Sewa Per Hari", "Tersedia", "Kategori"},
                0);
        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        PenyewaanMobil penyewaan = new PenyewaanMobil(daftarMobil, rentalApi);

        tampilkanButton.addActionListener(e -> {
            tableModel.setRowCount(0);
            penyewaan.tampilkanMobil(tableModel);
        });


        sewaButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1) {
                String namaMobil = (String) tableModel.getValueAt(selectedRow, 0);
                String lamaSewaStr = JOptionPane.showInputDialog("Masukkan lama sewa (hari):");
                if (lamaSewaStr != null) {
                    try {
                        int lamaSewa = Integer.parseInt(lamaSewaStr);
                        penyewaan.sewaMobil(namaMobil, lamaSewa);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(null, "Masukkan lama sewa yang valid (angka).");
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "Pilih mobil terlebih dahulu.");
            }
        });

        tampilkanStrukButton.addActionListener(e -> penyewaan.tampilkanDanKelolaStruk());

        keluarButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(null, "Terima kasih telah menggunakan layanan penyewaan mobil.");
            System.exit(0);
        });

        panel.add(tampilkanButton);
        panel.add(sewaButton);
        panel.add(tampilkanStrukButton);
        panel.add(keluarButton);

        frame.getContentPane().add(BorderLayout.NORTH, panel);
        frame.getContentPane().add(BorderLayout.CENTER, scrollPane);

        frame.setSize(400, 300);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new AplikasiPenyewaanMobilGUI();
    }
}