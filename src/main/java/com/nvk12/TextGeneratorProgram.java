package com.nvk12;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class TextGeneratorProgram {

	/**
	 * Точка входу програми
	 */
	public static void main(String... args) throws IOException {

		// Ініціалізація компоненту, який відповідає за логіку генерування
		// текстів
		TextGenerator generator = new TextGenerator("sentences.txt");

		// Ініціалізація графічного інтерфейсу програми
		GraphicalInterface graphicalInterface = new GraphicalInterface();
		graphicalInterface.initialize();
		graphicalInterface.setTextGenerator(generator);

		// Відображення графічного інтерфейсу на екрані
		graphicalInterface.display();
	}

	/**
	 * Програмний компонент, який містить логіку генерування тексту з числового
	 * коду
	 */
	private static class TextGenerator {

		// Символ, з допомогою якого поєднуються згенеровані речення
		private static final String DELIMITER = " ";

		// Таблиця, в якій зберігається інформація
		// про відповідність номерів та речень
		private Map<Integer, String> numberToSentence = new HashMap<>();

		/**
		 * Ініціалізація. Вхідними даними є шлях до файлу з реченнями.
		 */
		public TextGenerator(String fileWithSentences) throws IOException {

			// Зчитування даних про відповідність номерів та речень
			// з текстового файлу (кодування: UTF-8)
			try (BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(fileWithSentences), "UTF-8"))) {

				String line;
				while ((line = br.readLine()) != null) {
					// Зчитуються лише ті рядки, які мають формат:
					// Число (з крапкою), після якого слідує речення
					if (line.matches("\\d+\\..+")) {
						// Зчитування числа
						String numStr = line.replaceAll("(\\d+)\\.\\s+.+", "$1");
						int num = Integer.parseInt(numStr);

						// Зчитування речення
						String sentence = line.replaceAll("\\d+\\.\\s+(.+)", "$1");

						// Збереження речення під відповідним числом у таблицю
						this.numberToSentence.put(num, sentence);
					}
				}
			}
		}

		/**
		 * Функція, яка генерує текст. Вхідними даними є список чисел.
		 */
		public String compose(List<Integer> numbers) {
			// Змінняа, яка відповідає за побудову згенерованого тексту
			StringBuilder sb = new StringBuilder();

			// Перелічення усіх чисел зі списку
			for (Integer num : numbers) {
				// Для кожного числа - знаходиться відповідне речення
				// (у таблиці відповідності номерів та речень)
				String sentence = this.numberToSentence.get(num);
				if ((sentence != null) && !sentence.isEmpty()) {
					// Приєднання знайденого речення до тексту
					sb.append(sentence).append(DELIMITER);
				}
			}
			// Результатом є згенерований текст
			return sb.toString();
		}
	}

	/**
	 * Програмний компонент, який відповідає за створення графічного інтерфейсу
	 * програми.
	 */
	public static class GraphicalInterface {

		// Прив'язка логіки генерування текстів до графічного інтерфейсу
		private TextGenerator textGenerator;

		// Головне вікно програми
		private JFrame appWindow;

		// Текстове поле для введення числового коду
		private JTextField numbersInputField;

		// Кнопка, яка запускає логіку генерування тексту
		private JButton generateButton;

		// Текстове поле для відображення згенерованого тексту
		private JTextArea composedTextArea;

		// Компонент, який допомагає у првильній реалізації функціональних
		// можливосей графічного інтерфейсу
		private ExecutorService exector = Executors.newSingleThreadExecutor();

		/**
		 * Ініціалізація графічного інтерфейсу (розмір вікна, розміщення
		 * елементів керування і т.д.)
		 */
		public void initialize() {
			this.appWindow = new JFrame("Генератор текстів");
			this.appWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.appWindow.setSize(600, 600);
			this.appWindow.setLocationRelativeTo(null);
			this.appWindow.setLayout(new GridBagLayout());

			GridBagConstraints c = new GridBagConstraints();
			c.fill = 2;

			this.numbersInputField = new JTextField();
			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 0.9;
			this.appWindow.add(this.numbersInputField, c);

			this.generateButton = new JButton("OK");
			c.gridx = 1;
			c.gridy = 0;
			c.weightx = 0.1;
			this.appWindow.add(this.generateButton, c);

			// Ініціалізація логіки обробки введеного користувачем числового
			// коду - після натиснення на кнопку генерування тексту
			this.generateButton.addActionListener(this.generateButtonHandler());

			this.composedTextArea = new JTextArea(5, 10);
			this.composedTextArea.setLineWrap(true);
			this.composedTextArea.setWrapStyleWord(true);
			JScrollPane scrollPane = new JScrollPane(this.composedTextArea);
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 2;
			c.weighty = 1;
			c.fill = 1;
			c.insets = new Insets(2, 2, 2, 2);
			this.appWindow.add(scrollPane, c);
		}

		public void display() {
			this.appWindow.setVisible(true);
		}

		private ActionListener generateButtonHandler() {
			return new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {

					// На момент, поки програма генерує текст - елементи
					// керування робляться недоступними для користувача
					GraphicalInterface.this.disableControls();

					// Запуск фонового процесу генерування тексту
					GraphicalInterface.this.exector.submit(new Runnable() {
						@Override
						public void run() {

							// Зчитування числового коду, введеного користувачем
							String numbersStr = GraphicalInterface.this.numbersInputField.getText();

							// Усі введені числа повинні бути розділені
							// пробільними символами
							String[] numStrArr = numbersStr.split("\\s+");

							// Перетворення числового коду - у список чисел
							List<Integer> numbers = new ArrayList<>();
							for (String numStr : numStrArr) {
								try {
									int num = Integer.parseInt(numStr);
									numbers.add(Integer.valueOf(num));
								}
								catch (Exception e) {
									e.printStackTrace();
								}
							}

							// Запуск логіки генерування тексту зі списка чисел
							final String text = GraphicalInterface.this.textGenerator.compose(numbers);

							// Відображення згенерованого тексту в графічному
							// інтерфейсі
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									GraphicalInterface.this.composedTextArea.setText(text);

									// Елементи керування знову робляться
									// доступними для користувача
									GraphicalInterface.this.enableControls();
								}
							});
						}
					});
				}
			};
		}

		/**
		 * Функція, яка робить елементи графічного інтерфейсу недоступними для
		 * користувача
		 */
		public void disableControls() {
			this.numbersInputField.setEnabled(false);
			this.generateButton.setEnabled(false);
			this.composedTextArea.setEnabled(false);
		}

		/**
		 * Функція, яка робить елементи графічного інтерфейсу доступними для
		 * користувача
		 */
		public void enableControls() {
			this.numbersInputField.setEnabled(true);
			this.generateButton.setEnabled(true);
			this.composedTextArea.setEnabled(true);
		}

		/**
		 * Функція прив'язки логіки генерування тексту до графічного інтерфейсу
		 */
		public void setTextGenerator(TextGenerator textComposer) {
			this.textGenerator = textComposer;
		}
	}
}
