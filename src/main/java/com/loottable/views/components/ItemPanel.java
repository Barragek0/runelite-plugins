package com.loottable.views.components;

import java.net.HttpURLConnection;
import java.net.URL;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.Color;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.loottable.helpers.ScrapeWiki;
import com.loottable.helpers.UiUtilities;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

public class ItemPanel extends JPanel {
    private static final long serialVersionUID = 8426321039456174778L;
    String imageSource;
    String itemName;
    String quantity;
    String rarity;
    String price;

	public ItemPanel(String imageSource, String itemName, String quantity, String rarity, String price) {
        this.imageSource = imageSource;
        this.itemName = itemName;
        this.quantity = quantity;
        this.rarity = rarity;
        this.price = price;
        setBorder(new EmptyBorder(0, 0, 5, 0));
        setLayout(new BorderLayout());

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        container.setBorder(UiUtilities.ITEM_BORDER);
        
        JPanel leftPanel = constructLeftSide();
        JPanel rightPanel = constructRightSide();

        container.add(leftPanel, BorderLayout.WEST);
        container.add(rightPanel, BorderLayout.EAST);

        add(container);
    }

    private JPanel constructItemImage() {
        JPanel container = new JPanel(new BorderLayout());
        container.setSize(30, container.getHeight());

        try {
            // HttpRequest request = HttpRequest.newBuilder()
            //     .uri(URI.create("https://example.com/"))
            //     .timeout(Duration.ofMinutes(2))
            //     .header("Content-Type", "application/json")
            //     .POST(BodyPublishers.ofFile(Paths.get("file.json")))
            //     .build();
            // client.sendAsync(request, BodyHandlers.ofString())
            //     .thenApply(HttpResponse::body)
                // .thenAccept(System.out::println);

            URL url = new URL(this.imageSource);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty(
                "User-Agent",
                ScrapeWiki.userAgent
            );
            BufferedImage image = ImageIO.read(connection.getInputStream());
            JLabel label = new JLabel(new ImageIcon(image));
            label.setSize(35, label.getWidth());
            container.add(label, BorderLayout.WEST);
            container.setSize(35, container.getHeight());
            return container;
        } catch (Exception error) {
            return container;
        }
    }

    /**
     * Constructs left side of item panel
     * Item Name
     * Rarity
     */
    private JPanel constructLeftSide() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        JPanel itemImage = constructItemImage();

        JPanel leftSidePanel = new JPanel(new GridLayout(2, 1));
        leftSidePanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        JLabel itemNameLabel = new JLabel(itemName);
        itemNameLabel.setBorder(new EmptyBorder(0, 0, 3, 0));
        itemNameLabel.setFont(FontManager.getRunescapeBoldFont());
        itemNameLabel.setHorizontalAlignment(JLabel.LEFT);
        itemNameLabel.setVerticalAlignment(JLabel.CENTER);

        JLabel rarityLabel = new JLabel(rarity);
        rarityLabel.setHorizontalAlignment(JLabel.LEFT);
        rarityLabel.setVerticalAlignment(JLabel.CENTER);

        leftSidePanel.add(itemNameLabel);
        leftSidePanel.add(rarityLabel);

        container.add(itemImage);
        container.add(leftSidePanel);
        return container;
    }


    /**
     * Constructs right side of item panel
     * quantity
     * Price
     */
    private JPanel constructRightSide() {
        JPanel rightSidePanel = new JPanel(new GridLayout(2, 1));
        // rightSidePanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        JLabel quantityLabel = new JLabel("x" + quantity);
        quantityLabel.setBorder(new EmptyBorder(0, 0, 3, 0));
        quantityLabel.setHorizontalAlignment(JLabel.RIGHT);
        quantityLabel.setVerticalAlignment(JLabel.CENTER);

        JLabel priceLabel = price == "Not sold" ? new JLabel(price) : new JLabel(price + "gp");
        priceLabel.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);
        priceLabel.setVerticalAlignment(JLabel.CENTER);

        rightSidePanel.add(quantityLabel);
        rightSidePanel.add(priceLabel);

        return rightSidePanel;
    }
}
