package com.example.lesson3operationswithfiles;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // ----- Class constants -----------------------------------------------
    private final static String TAG = "===== MainActivity";

    // ----- Class members -------------------------------------------------
    /*
     * Поля, относящиеся к примеру на Диалоговое окно android.app.AlertDialog
     * для выбора и открытия файлов, находящихся на внешнем носителе.
     * ----------------------------------------------------------------------
     */
    /**
     * Виджет android.widget.ListView, который будет размещаться в Диалоговом
     * окне выбора файлов android.app.AlertDialog и содержать список файлов
     * и каталогов внешнего носителя.
     */
    private ListView lvFiles;

    /**
     * Адаптер Данных для списка lvFiles.
     */
    private ArrayAdapter<String> adapter;

    /**
     * Текущий отображаемый в списке lvFiles каталог внешнего носителя.
     */
    private File esCurDir;

    /**
     * Текущий открытый пользователем файл, который отображается в
     * приложении.
     */
    private File esCurFile;

    /**
     * Ссылка на Диалоговое окно android.app.AlertDialog, которое
     * будет предоставлять пользователю возможность перемещаться по каталогам
     * внешнего носителя и выбирать файлы для отображения их содержимого
     * в приложении.
     */
    private AlertDialog dialog;

    // ----- Class methods -------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
         * Проверка, что у приложения есть разрешения на чтение/запись
         * файлов на внешнем носителе!
         * Если разрешения нет - программно его создать, предварительно
         * запросив подтверждение пользователя.
         * ----------------------------------------------------------------------
         */
        int permission = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Разрешения нет, запросим это разрешение у пользователя");

            // ----- Разрешения нет, запросим это разрешение у пользователя --------
            ActivityCompat.requestPermissions(
                    this,
                    new String[]
                            {
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                            }, 1);
        }


        if (this.isExternalStorageWritable()) {
            // ----- Получение пути к каталогу внешнего носителя -------------------
            File esMainDir = Environment.getExternalStorageDirectory();
            Log.d(MainActivity.TAG, "Путь к каталогу внешнего носителя : " +
                    esMainDir.getAbsolutePath());
        } else {
            Log.d(MainActivity.TAG, "Устройство не готово!");
        }

        /*
         * Формирование Диалогового окна android.app.AlertDialog
         * для открытия файлов.
         * Это Диалоговое окно, в котором будет отображаться список
         * названий каталогов и файлов находящихся на внешнем носителе.
         * Пользователь может перемещаться по каталогам внешнего носителя.
         * ----------------------------------------------------------------------
         */
        // ----- Получение пути к каталогу внешнего носителя -------------------
        this.esCurDir = Environment.getExternalStorageDirectory();

        // ----- Получение списка каталогов и файлов внешнего носителя ---------
        ArrayList<String> listFiles = this.fillDirectory(this.esCurDir);

        // ----- Формирование android.app.AlertDialog --------------------------
        AlertDialog.Builder builder = new AlertDialog.Builder(
                this);
        // ----- D заголовке окна будет отображать каталог в котором сейчас находимся
        builder.setTitle(this.esCurDir.getAbsolutePath());

        // ----- Список с именами файлов и каталогов для размещения в Диалоговом окне
        this.lvFiles = new ListView(this);
        this.adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, listFiles);
        this.lvFiles.setAdapter(this.adapter);

        // ----- Обработчик события клика по элементу списка -------------------
        this.lvFiles.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = MainActivity.this.adapter.getItem(position);
                item = item.replaceAll("[\\[\\]]", "");

                // ----- Если пользователь кликнул по [..] то выходим в родительский каталог
                File fileObj = (item.compareTo("..") == 0) ?
                        (MainActivity.this.esCurDir.getParentFile()) :
                        (new File(MainActivity.this.esCurDir, item));

                if (fileObj.isDirectory()) {
                    // ----- Пользователь кликнул по каталогу - зайдем в него --------------
                    MainActivity.this.adapter.clear();

                    ArrayList<String> listFiles = MainActivity.this.fillDirectory(fileObj);

                    // ----- Для корневого каталога внешнего носителя не добавляем [..] ----
                    if (fileObj.compareTo(Environment.getExternalStorageDirectory()) != 0) {
                        MainActivity.this.adapter.add("[..]");
                    }

                    MainActivity.this.adapter.addAll(listFiles);
                    MainActivity.this.esCurDir = fileObj;
                    MainActivity.this.dialog.setTitle(MainActivity.this.esCurDir.getAbsolutePath());
                } else if (fileObj.isFile()) {
                    // ----- Пользователь выбрал файл - прочитаем этот файл ----------------
                    try {
                        LineNumberReader LR = new LineNumberReader(new FileReader(fileObj));
                        String S = "";
                        while (true) {
                            String z = LR.readLine();
                            if (z == null) break;
                            S += z + "\n";
                        }
                        LR.close();

                        // ----- Отобразим содержимое файла в виджете android.widget.EditText --
                        EditText etEditFile = (EditText) MainActivity.this.findViewById(R.id.etEditFile);
                        etEditFile.setText(S);

                        // ----- Запомним путь к файлу -----------------------------------------
                        MainActivity.this.esCurFile = fileObj;
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this,
                                "Ошибка открытия файла : \n" +
                                        e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.d(MainActivity.TAG,
                                "Ошибка открытия файла : " + e.getMessage());
                    }

                    // ----- Программно закроем Диалоговое окно ----------------------------
                    MainActivity.this.dialog.dismiss();

                    Toast.makeText(MainActivity.this, fileObj.getAbsolutePath(), Toast.LENGTH_LONG).show();
                }
            }
        });

        // ----- Делаем содержимым Диалогового окна список android.widget.ListView
        builder.setView(this.lvFiles);

        // ----- Кнопки Диалогового окна ---------------------------------------
        builder.setNegativeButton("Закрыть", null);

        // ----- Создание Диалогового окна - один раз в приложении -------------
        this.dialog = builder.create();

    }

    /**
     * Метод возвращает коллекцию названий файлов и каталогов
     * внешнего носителя, которые находятся в каталоге dir.
     *
     * @param dir - Каталог внешнего носителя, список файлов и
     *            подкаталогов которого необходимо получить.
     * @return        - Коллекцию любого размера строк. Имена каталогов
     * заключены в [квадратные скобки].
     */
    private ArrayList<String> fillDirectory(File dir) {
        ArrayList<String> listFiles = new ArrayList<>();

        if (this.isExternalStorageReadable()) {
            File[] arrFiles = dir.listFiles();
            if (arrFiles != null) {
                for (File f : arrFiles) {
                    if (f.isDirectory()) {
                        listFiles.add("[" + f.getName() + "]");
                    } else {
                        listFiles.add(f.getName());
                    }
                }
            }
        }
        return listFiles;
    }

    /**
     * Метод проверяет готовность внешнего носителя для операций чтения/записи.
     * ----------------------------------------------------------------------
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return (state.equals(Environment.MEDIA_MOUNTED));
    }

    /**
     * Метод проверяет готовность внешнего носителя для операций чтения.
     * ----------------------------------------------------------------------
     */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (state.equals(Environment.MEDIA_MOUNTED) ||
                state.equals(Environment.MEDIA_MOUNTED_READ_ONLY));
    }

    /**
     * Обрабочик события клика на кнопку "Список файлов".
     * Метод получает список файлов и подкаталогов внешнего носителя
     * и выводит их в Диалоговом окне.
     *
     * @param v - Ссылка на виджет, который является источником события.
     */
    public void btnListFilesClick(View v) {

        if (this.isExternalStorageReadable()) {

            // ----- Получение пути к каталогу внешнего носителя -------------------
            File esMainDir = Environment.getExternalStorageDirectory();

            Log.d(MainActivity.TAG, "Путь к каталогу внешнего носителя : " +
                    esMainDir.getAbsolutePath());

            // ----- Получение списка файлов и подкаталогов ------------------------
            ArrayList<String> listFiles = new ArrayList<>();
            File[] arrFiles = esMainDir.listFiles();
            if (arrFiles != null) {
                for (File f : arrFiles) {
                    if (f.isDirectory()) {
                        listFiles.add("[" + f.getName() + "]");
                    } else {
                        listFiles.add(f.getName());
                    }
                }
            } else {
                Toast.makeText(this, "Каталог внешнего носителя пуст!", Toast.LENGTH_SHORT).show();
            }

            // ----- Вывод в Log найденных на внешнем носителе файлов и каталогов --
            for (int i = 0; i < listFiles.size(); i++) {
                Log.d(TAG, listFiles.get(i));
            }// ----- Создание объекта AlertDialog.Builder для формирования Диалогового окна
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    this, android.R.style.Theme_Holo_Light_Dialog);
            builder.setTitle("Список файлов и каталогов внешнего носителя");

            ListView LV = new ListView(this);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_list_item_1, listFiles);
            LV.setAdapter(adapter);

            builder.setView(LV);

            builder.setNegativeButton("Закрыть", null);

            AlertDialog dialog = builder.create();
            dialog.show();

        } else {
            Toast.makeText(this, "Ошибка: Внешний носитель не готов!", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Обрабочик события клика на кнопку "Создать файл".
     * Метод отображает Диалоговое окно со списком одиночного выбора
     * android.widget.ListView и двумя текстовыми полями android.widget.EditView.
     * В списке ListView пользователь выбирает каталог в котором он хочет создать файл.
     * В текстовых полях пользователь вводит название создаваемого файла и текстовое
     * содержиимое создаваемого файла.
     *
     * @param v - Ссылка на виджет, который является источником события.
     */
    public void btnCreateFileClick(View v) {
        if (this.isExternalStorageWritable()) {
            // ----- Получение пути к каталогу внешнего носителя -------------------
            File esMainDir = Environment.getExternalStorageDirectory();

            // ----- Получение списка файлов и подкаталогов ------------------------
            ArrayList<String> listFiles = this.fillDirectory(esMainDir);

            // ----- Создание объекта AlertDialog.Builder для формирования Диалогового окна
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    this, android.R.style.Theme_Holo_Light_Dialog);
            builder.setTitle("Создать файл");

            // ----- Создание виджета для содержимого Диалогового окна с помощью Inflater
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.create_file_dialog_content,
                    null, false);

            // ----- Создание Адаптера данных для списка ListView из контента для Диалога
            ArrayAdapter<String> A = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_single_choice, listFiles);

            ListView lvDirList = (ListView) view.findViewById(R.id.lvDirList);
            lvDirList.setAdapter(A);

            // ----- Сообщаем, что ListView это список одиночного выбора -----------
            lvDirList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            // ----- Назначаем контент Диалоговому окну ----------------------------
            builder.setView(view);

            // ----- Кнопки "Создать" и "Отменить" для Диалогового окна ------------
            builder.setPositiveButton("Создать", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    File esMainDir = Environment.getExternalStorageDirectory();

                    // ----- Определяем выбранный пользователем каталог --------------------
                    ListView lvDirList = (ListView)
                            ((AlertDialog) dialog).findViewById(R.id.lvDirList);

                    int index = lvDirList.getCheckedItemPosition();
                    String strDir = "";
                    if (index != -1) {
                        strDir = lvDirList.getAdapter().getItem(index).toString();
                        strDir = strDir.replaceAll("[\\[\\]]", "");
                    }
                    File dir = (strDir.isEmpty()) ?
                            esMainDir : (new File(esMainDir, strDir));

                    // ----- Читаем из текстового поля название создаваемого файла ---------
                    EditText etFileName = (EditText)
                            ((AlertDialog) dialog).findViewById(R.id.etFileName);
                    String strFileName = etFileName.getText().toString();
                    if (strFileName.isEmpty()) {
                        // ----- Если пользователь не ввел имя файла - создадим случайное название
                        strFileName = "noname" + ((int) (Math.random() * 100)) + ".txt";
                    }

                    // ----- Читаем из текстового поля содержимое для записи в создаваемый файл
                    EditText etFileCont = (EditText)
                            ((AlertDialog) dialog).findViewById(R.id.etFileContent);
                    String strContent = etFileCont.getText().toString();
                    if (strContent.isEmpty()) {
                        // ----- Если пользователь не ввел содержимое файла, то будет строка "no content"
                        strContent = "no content";
                    }

                    // ----- Создаем файл и записываем в него содержимое -------------------
                    try {
                        File fileName = new File(dir, strFileName);

                        FileWriter f = new FileWriter(fileName);
                        f.write(strContent + "\r\n");
                        f.flush();
                        f.close();

                        Toast.makeText(MainActivity.this,
                                "Файл создан успешно: \n" + strFileName,
                                Toast.LENGTH_LONG).show();
                    } catch (IOException ioe) {
                        Toast.makeText(MainActivity.this,
                                "Ошибка записи в файл: \n" + ioe.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.d(MainActivity.TAG, "Ошибка записи в файл: " + ioe.getMessage());
                    }
                }
            });

            builder.setNegativeButton("Отменить", null);

            // ----- Создание объекта android.app.AlertDialog ----------------------
            AlertDialog dialog = builder.create();

            // ----- Отображение Диалогового окна ----------------------------------
            dialog.show();

            // -----
        } else {
            Toast.makeText(this, "Ошибка: Внешний носитель не готов!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Обрабочик события клика на кнопку "Открыть файл".
     * Метод отображает Диалоговое окно для перемещения по каталогам
     * внешнего носителя и выбора файла для отображения его содержимого в
     * приложении.
     * Диалоговое окно, отображаемое в этом методе, создается один
     * раз в методе onCreate.
     *
     * @param v - Ссылка на виджет, который является источником события.
     */
    public void btnOpenFileClick(View v) {
        if (this.isExternalStorageWritable()) {
            this.dialog.show();
        } else {
            Toast.makeText(this, "Внешний носитель не готов", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Обрабочик события клика на кнопку "Сохранить файл".
     * Метод выполняет сохранение содержимого ранее открытого файла
     * из виджета android.widget.EditText (идентификатор R.id.etEditFile)
     * обратно в этот же файл на внешнем носителе.
     *
     * @param v - Ссылка на виджет, который является источником события.
     */
    public void btnSaveFileClick(View v) {
        // ----- Проверка, что файл был открыт ---------------------------------
        if (this.esCurFile == null) {
            Toast.makeText(this,
                    "Для сохранения необходимо предварительно выбрать и отркыть файл",
                    Toast.LENGTH_LONG).show();
        }

        // ----- Если внешний носитель доступен для записи ---------------------
        if (this.isExternalStorageWritable()) {
            // ----- Получаем текст из текстового поля android.widget.EditText -----
            EditText etEditFile = (EditText) this.findViewById(R.id.etEditFile);
            String content = etEditFile.getText().toString();

            // ----- Создаем файл и записываем в него содержимое -------------------
            try {
                FileWriter f = new FileWriter(this.esCurFile);
                f.write(content + "\r\n");
                f.flush();
                f.close();

                Toast.makeText(MainActivity.this,
                        "Файл сохранен успешно: \n" + this.esCurFile.getAbsolutePath(),
                        Toast.LENGTH_LONG).show();
            } catch (IOException ioe) {
                Toast.makeText(MainActivity.this,
                        "Ошибка записи в файл: \n" + ioe.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.d(MainActivity.TAG, "Ошибка записи в файл: " + ioe.getMessage());
            }
        } else {
            Toast.makeText(this, "Внешний носитель не готов", Toast.LENGTH_SHORT).show();
        }
    }
}
