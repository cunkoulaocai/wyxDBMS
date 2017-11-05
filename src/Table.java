import java.io.*;
import java.util.*;

public class Table {
    private String name;//表名
    private File folder;//表所在的文件夹
    private File dictFile;//数据字典
    private File dataFile;//数据
    private Map<String, Field> fieldMap;//字段映射集
    private List<Map<Integer, String>> resultDatas;//where语句过滤后(或者不使用where)的数据结果集合 key为索引(元组所在的行号)
    private static String userName;//用户姓名，切换或修改用户时修改
    private static String dbName;//数据库dataBase名，切换时修改

    /**
     * 只能静态创建，所以构造函数私有
     */
    private Table(String name) {
        this.name = name;
        this.fieldMap = new LinkedHashMap();
        this.folder = new File("/Users/ouhikoshin/IdeaProjects/wyxDBMS/dir" + "/" + userName + "/" + dbName + "/" + name);
        this.dictFile = new File(folder, name + ".dict");
        this.dataFile = new File(folder + "/data", 1 + ".data");
    }


    /**
     * 初始化表信息，包括用户和数据库
     *
     * @param userName 用户名
     * @param dbName   数据库名
     */
    public static void init(String userName, String dbName) {
        Table.userName = userName;
        Table.dbName = dbName;
    }

    /**
     * 创建一个新的表文件
     *
     * @param name 表名
     * @return 如果表存在返回失败的信息，否则返回success
     */
    public static String createTable(String name, Map<String, Field> fields) {
        if (existTable(name)) {
            return "创建表失败，因为已经存在表:" + name;
        }
        Table table = new Table(name);


        table.dictFile.getParentFile().mkdirs();//创建真实目录

        table.addDict(fields);
        return "success";
    }


    /**
     * 根据表名获取表
     *
     * @param name 表名
     * @return 如果不存在此表返回null, 否则返回对应Table对象
     */
    public static Table getTable(String name) {
        if (!existTable(name)) {
            return null;
        }
        Table table = new Table(name);
        try (
                FileReader fr = new FileReader(table.dictFile);
                BufferedReader br = new BufferedReader(fr)
        ) {

            String line = null;
            //读到末尾是NULL
            while (null != (line = br.readLine())) {
                String[] fieldValues = line.split(" ");//用空格产拆分字段
                Field field = new Field();
                field.setName(fieldValues[0]);
                field.setType(fieldValues[1]);
                //如果长度为3说明此字段是主键
                if ("*".equals(fieldValues[2])) {
                    field.setPrimaryKey(true);
                } else {
                    field.setPrimaryKey(false);
                }
                //将字段的名字作为key
                table.fieldMap.put(fieldValues[0], field);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return table;
    }

    public Map<String, Field> getFieldMap() {
        return fieldMap;
    }


    private static void deleteFolder(File file) {
        if (file.isFile()) {//判断是否是文件
            file.delete();//删除文件
        } else if (file.isDirectory()) {//否则如果它是一个目录
            File[] files = file.listFiles();//声明目录下所有的文件 files[];
            for (int i = 0; i < files.length; i++) {//遍历目录下所有的文件
                deleteFolder(files[i]);//把每个文件用这个方法进行迭代
            }
            file.delete();//删除文件夹
        }
    }


    public static String dropTable(String name) {
        if (!existTable(name)) {
            return "错误：不存在表:" + name;
        }
        File folder = new File("/Users/ouhikoshin/IdeaProjects/wyxDBMS/dir" + "/" + userName + "/" + dbName + "/" + name);
        deleteFolder(folder);
        return "success";

    }

    /**
     * 判断表是否存在
     *
     * @param name 表名
     * @return 存在与否
     */
    public static boolean existTable(String name) {
        File folder = new File("/Users/ouhikoshin/IdeaProjects/wyxDBMS/dir" + "/" + userName + "/" + dbName + "/" + name);
        return folder.exists();
    }

    /**
     * 在字典文件中写入创建的字段信息,然后将新增的字段map追加到this.fieldMap
     *
     * @param fields 字段列表，其中map的name为列名，type为数据类型，primaryKey为是否作为主键
     * @return
     */
    public String addDict(Map<String, Field> fields) {
        Set<String> keys = fields.keySet();
        for (String key : keys) {
            if (fieldMap.containsKey(key)) {
                return "错误：存在重复添加的字段:" + key;
            }
        }
        writeDict(fields, true);

        fieldMap.putAll(fields);

        return "success";
    }

    /**
     * 在数据文件没有此字段的数据的前提下，可以删除此字段
     *
     * @param fieldName 字段名
     * @return
     */
    public String deleteDict(String fieldName) {
        if (!fieldMap.containsKey(fieldName)) {
            return "错误：不存字段：" + fieldName;
        }
        fieldMap.remove(fieldName);
       /* //如果删除了最后一条字段，则删除整个表文件
        if (0 == fieldMap.size()) {
            dropTable(this.name);
        } else {
        }*/
        writeDict(fieldMap, false);

        return "success";
    }

    /**
     * 提供一组字段写入文件
     *
     * @param fields 字段映射集
     * @param append 是否在文件结尾追加
     */
    private void writeDict(Map<String, Field> fields, boolean append) {
        try (
                FileWriter fw = new FileWriter(dictFile, append);
                PrintWriter pw = new PrintWriter(fw)
        ) {
            //for (Map.Entry<String, Field> fieldEntry : fields.entrySet()) {
            for (Field field : fields.values()) {
                String name = field.getName();
                String type = field.getType();
                //如果是主键字段后面加*
                if (field.isPrimaryKey()) {
                    pw.println(name + " " + type + " " + "*");
                } else {//非主键^
                    pw.println(name + " " + type + " " + "^");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 对空位填充fillStr,填充后的字段按照数据字段顺序排序
     *
     * @param fillStr 要填充的字符串
     * @param data    原始数据
     * @return 填充后的数据
     */
    private Map<String, String> fillData(Map<String, String> data, String fillStr) {
        //fillData是真正写入文件的集合，空位补fillStr;
        Map<String, String> fillData = new LinkedHashMap<>();
        //遍历数据字典
        for (Map.Entry<String, Field> fieldEntry : fieldMap.entrySet()) {
            String fieldKey = fieldEntry.getKey();
            if (null == data.get(fieldKey)) {
                fillData.put(fieldKey, fillStr);
            } else {
                fillData.put(fieldKey, data.get(fieldKey));
            }
        }
        return fillData;
    }

    /**
     * 利用正则表达式判断data类型是否与数据字典相符
     *
     * @param data
     * @return
     */
    private boolean checkType(Map<String, String> data) {
        //如果长度不一致，返回false
        if (data.size() != fieldMap.size()) {
            return false;
        }

        //遍历data.value和field.type,逐个对比类型
        //Iterator<String> dataIter = data.values().iterator();
        Iterator<Field> fieldIter = fieldMap.values().iterator();

        while (fieldIter.hasNext()) {
            //String dataValue = dataIter.next();
            Field field = fieldIter.next();
            String dataValue = data.get(field.getName());
            //如果是[NULL]则跳过类型检查
            if ("[NULL]".equals(dataValue)) {
                continue;
            }

            switch (field.getType()) {
                case "int":
                    if (!dataValue.matches("^(-|\\+)?\\d+$")) {
                        return false;
                    }
                    break;
                case "double":
                    if (!dataValue.matches("^(-|\\+)?\\d*\\.?\\d+$")) {
                        return false;
                    }
                    break;
                case "varchar":
                    break;
                default:
                    return false;
            }
        }
        return true;
    }



    /**
     * 在插入时，对语法进行检查，并对空位填充[NULL],默认插入为追加方式
     *
     * @param srcData
     * @return
     */
    public String insert(Map<String, String> srcData) {
        return insert(srcData, true);
    }

    /**
     * 在插入时，对语法进行检查，并对空位填充[NULL]
     *
     * @param append  是否追加
     * @param srcData 未处理的原始数据
     * @return
     */
    private String insert(Map<String, String> srcData, boolean append) {
        if (srcData.size() > fieldMap.size() || 0 == srcData.size()) {
            return "错误：插入数据失败，请检查语法";
        }

        //遍历数据字典,查看主键是否为空
        for (Map.Entry<String, Field> fieldEntry : fieldMap.entrySet()) {
            String fieldKey = fieldEntry.getKey();
            Field field = fieldEntry.getValue();
            //如果此字段是主键,不可以为null
            if (field.isPrimaryKey()) {
                if (null == srcData.get(fieldKey) || "[NULL]".equals(srcData.get(fieldKey))) {
                    return "错误：字段:" + fieldKey + "是主键，不能为空";
                }
            }
        }
        Map<String, String> insertData = fillData(srcData, "[NULL]");
        if (!checkType(insertData)) {
            return "错误：检查插入的类型";
        }

        dataFile.getParentFile().mkdirs();
        try (
                FileWriter fw = new FileWriter(dataFile, append);
                PrintWriter pw = new PrintWriter(fw)
        ) {
            StringBuilder line = new StringBuilder();
            for (String value : insertData.values()) {
                line.append(value).append(" ");
            }
            pw.println(line.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return "写入异常";
        }
        return "success";
    }



    /**
     * 读取指定文件的所有数据
     *
     * @param dataFile 数据文件
     * @return 数据列表
     */
    public List<Map<String, String>> readDatas(File dataFile) {
        List<Map<String, String>> dataMapList = new ArrayList<>();

        try (
                FileReader fr = new FileReader(dataFile);
                BufferedReader br = new BufferedReader(fr)
        ) {

            String line = null;
            while (null != (line = br.readLine())) {
                Map<String, String> dataMap = new LinkedHashMap<>();
                String[] datas = line.split(" ");
                Iterator<String> fieldNames = getFieldMap().keySet().iterator();
                for (String data : datas) {
                    String dataName = fieldNames.next();
                    dataMap.put(dataName, data);
                }
                dataMapList.add(dataMap);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataMapList;
    }


    private void writeDatas(File dataFile, List<Map<String, String>> datas) {
        if (dataFile.exists()) {
            dataFile.delete();
        }
        for (Map<String, String> data : datas) {
            insert(data);
        }
    }


    /**
     * 根据给定的过滤器组，查找索引，将指定的文件数据删除
     * @param singleFilters 过滤器组
     */
    public void delete(List<SingleFilter> singleFilters) {
        //此处查找索引
        deleteData(this.dataFile, singleFilters);
    }

    /**
     * 读取给定文件，读取数据并使用过滤器组过滤，将过滤后的写入文件
     * @param file 数据文件
     * @param singleFilters 过滤器组
     */
    private void deleteData(File file, List<SingleFilter> singleFilters) {
        //读取数据文件
        List<Map<String, String>> srcDatas = readDatas(dataFile);
        List<Map<String, String>> filtDatas = new ArrayList<>(srcDatas);
        //Collections.copy(filtDatas, srcDatas);
        for (SingleFilter singleFilter : singleFilters) {
            filtDatas = singleFilter.singleFiltData(filtDatas);
        }
        srcDatas.removeAll(filtDatas);
        writeDatas(file,srcDatas);
    }

    /**
     * 根据给定的过滤器组，查找索引，将指定的文件数据更新
     * @param updateDatas 更新的数据
     * @param singleFilters 过滤器组
     */
    public void update(Map<String, String> updateDatas,List<SingleFilter> singleFilters) {
        //此处查找索引
        updateData(this.dataFile,updateDatas,singleFilters);
    }

    /**
     * 读取给定文件，读取数据并使用过滤器组过滤，将过滤出的数据更新并写入文件
     * @param file 数据文件
     * @param updateDatas 更新的数据
     * @param singleFilters 过滤器组
     */
    private void updateData(File file,Map<String, String> updateDatas, List<SingleFilter> singleFilters) {
        //读取数据文件
        List<Map<String, String>> srcDatas = readDatas(dataFile);
        List<Map<String, String>> filtDatas = new ArrayList<>(srcDatas);
        //Collections.copy(filtDatas, srcDatas);
        //循环过滤
        for (SingleFilter singleFilter : singleFilters) {
            filtDatas = singleFilter.singleFiltData(filtDatas);
        }
        //将过滤的数据遍历，将数据的值更新为updateDatas对应的数据
        for (Map<String, String> filtData : filtDatas) {
            for (Map.Entry<String, String> setData : updateDatas.entrySet()) {
                filtData.put(setData.getKey(), setData.getValue());
            }
        }
//        srcDatas.removeAll(filtDatas);
        writeDatas(file,srcDatas);
    }


    public static void main(String[] args) {
        User user = new User("user1", "abc");
        //默认进入user1用户文件夹
        File userFolder = new File("/Users/ouhikoshin/IdeaProjects/wyxDBMS/dir", user.getName());

        //默认进入user1的默认数据库db1
        File dbFolder = new File(userFolder, "db1");


        Table.init(user.getName(), dbFolder.getName());
        Table table1 = Table.getTable("table1");

        System.out.println();
    }
}
