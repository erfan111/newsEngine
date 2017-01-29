package ir.open30stem.searchEngine;

import ir.open30stem.models.NewsItemRepository;
import ir.open30stem.models.newsItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jhazm.Stemmer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by erfan on 9/24/14.
 */
public class InvertedIndex {
    private HashMap<String, LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>>> thedictionary = new HashMap<>();
    private HashMap<String, Double[]> weights = new HashMap<>();
    private HashMap<String, Integer> doc_id = new HashMap<>();
    private HashMap<Integer, String> inverse_doc_id = new HashMap<>();
    private Integer N;
    private NewsItemRepository newsItemRepository;
    public boolean indexed;

    public InvertedIndex(NewsItemRepository newsItemRepository) {
        this.newsItemRepository = newsItemRepository;
        this.indexed = false;
    }

    // The starter method of inverted index, automatically creates the index from files in ../input/
    public void read() throws IOException {
        List<newsItem> list = newsItemRepository.findByFinishedOrIsModified(true, true);
        Integer master_counter;
        N = list.size();
        Integer n = 0;
        if(indexed) {
            doc_id.clear();
            inverse_doc_id.clear();
            thedictionary.clear();
            weights.clear();
        }
        for (newsItem ni : list) {
            doc_id.put(ni.id, n);
            inverse_doc_id.put(n, ni.id);
            n++;
        }

        System.out.println("Reading news:");
        for (int j = 0; j < N; j++) {
            newsItem news = list.get(j);
            System.out.println(news.id);
            System.out.println(j);
            System.out.println("----------------------");
            master_counter = 0;
            for (String final_word : news.stemmed) {
                if (!final_word.isEmpty()) {
                    master_counter++;
                    if (thedictionary.containsKey(final_word)) {
                        LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> ll = thedictionary.get(final_word);
                        boolean flag1 = false;
                        for (Tuple<Integer, Integer, ArrayList<Integer>> aRecord : ll) { // check if occurred in that document before
                            if (aRecord.x.equals(doc_id.get(news.id))) {
                                aRecord.y++;
                                aRecord.z.add(master_counter);
                                flag1 = true;
                                break;
                            }
                        }
                        if (!flag1) {
                            ArrayList<Integer> pos_list = new ArrayList<>();
                            pos_list.add(master_counter);
                            Tuple<Integer, Integer, ArrayList<Integer>> t = new Tuple<>(doc_id.get(news.id), 1, pos_list);
                            ll.addFirst(t);

                        }
                    } else {
                        ArrayList<Integer> pos_list = new ArrayList<>();
                        pos_list.add(master_counter);
                        LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> posting_list = new LinkedList<>();
                        posting_list.addFirst(new Tuple<>(doc_id.get(news.id), 1, pos_list));
                        thedictionary.put(final_word, posting_list);

                    }
                }
            }

            news.isIndexed = true;
            if(news.isModified)
                news.isModified = false;
            newsItemRepository.save(news);
        }
        boolean disk_flag = false;
        //noinspection ConstantConditions
        if (disk_flag) {
            disk_handler();
        }

        // calculate the weights
        for (String word : thedictionary.keySet()) {
            LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> ll = thedictionary.get(word);
            Double[] word_weights = new Double[N + 1];
            for (Tuple<Integer, Integer, ArrayList<Integer>> tpl : ll) {
                word_weights[tpl.x] = (double) tpl.y * Math.log10((double) N / (double) ll.size());
            }
            word_weights[0] = 0.00;
            for (int j = 0; j < N + 1; j++) {
                if (word_weights[j] == null) word_weights[j] = 0.00;
            }
            weights.put(word, word_weights);
        }
        indexed = true;
    }

    public List<newsItem> query(boolean weighted_flag, boolean new_method, String input, List<Double> inputWeight) throws IOException {
        ArrayList<newsItem> results = new ArrayList<>();
        Stemmer stemmer = new Stemmer();
        String[] user_query = input.split(" ");
        String[] stemmed_query = new String[user_query.length];
        for (int i = 0; i < user_query.length; i++) {
            stemmed_query[i] = stemmer.stem(user_query[i]);
        }
        PriorityQueue<RankTuple> ranklist;
        // for each term in user's query:
        if (new_method) {
            Comparator<RankTuple> comparator = new RankTupleComparator();
            ranklist = calc_rank_ratio(stemmed_query, inputWeight, comparator);
            if (ranklist.size() < 50) {
                for (int j = 1; j <= ranklist.size(); j++) {
                    System.out.println(j + ") " + inverse_doc_id.get(ranklist.poll().x));
                }
            } else {
                for (int j = 1; j <= 50; j++) {
                    System.out.println(j + ") " + inverse_doc_id.get(ranklist.poll().x));
                }
            }
        } else {
            Comparator<RankTuple> comparator = new TupleComparator();
            ranklist = calc_rank(stemmed_query, inputWeight, comparator, weighted_flag);
            RankTuple temp;
            Stack<RankTuple> output = new Stack<>();
            if (ranklist.size() < 20) {
                for (int j = 1; j <= ranklist.size(); j++) {
                    output.push(ranklist.poll());
                }
                for (int j = 1; j <= ranklist.size(); j++) {
                    temp = output.pop();
                    if (temp.y != 0){
                        newsItem item = newsItemRepository.findById(inverse_doc_id.get(temp.x));
                        results.add(item);
                    }
                }
            } else {
                for (int j = 1; j <= ranklist.size(); j++) {
                    output.push(ranklist.poll());
                }
                for (int j = 1; j <= 20; j++) {
                    temp = output.pop();
                    if (temp.y != 0) {
                        newsItem item = newsItemRepository.findById(inverse_doc_id.get(temp.x));
                        results.add(item);
                    }
                }
            }
        }
        return results;
    }

    private PriorityQueue<RankTuple> calc_rank(String[] q, List<Double> q_w, Comparator<RankTuple> tc, boolean weighted_query) {
        Double q_term_size = 1.00;
        PriorityQueue<RankTuple> result = new PriorityQueue<>(N, tc);
        ArrayList<LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>>> posting_lists = new ArrayList<>();
        for(String term : q){
            posting_lists.add(thedictionary.get(term));
        }
        ArrayList<Integer> documentsInvolved = unionPostings(posting_lists);
//        System.out.println(documentsInvolved.size() + " posting size");
        for (Integer document_id : documentsInvolved) {
            Double qdotd = 0.00;
            if (weighted_query) {
                for (int queryTermIndex = 0; queryTermIndex < q.length; queryTermIndex++) {
                    Double[] word_w = weights.get(q[queryTermIndex]);
                    if (word_w != null) qdotd += q_w.get(queryTermIndex) * word_w[document_id];
                }
            } else {
                for (String aQ : q) {
                    Double[] word_w = weights.get(aQ);
                    if (word_w != null) qdotd += word_w[document_id];
                }
            }
            Double sigmad2 = 0.00;
            for (Double[] dbl : weights.values()) {
                sigmad2 += (dbl[document_id] * dbl[document_id]);
            }
            if (weighted_query) {
                Double sigmaQ = 0.00;
                for (int qn = 0; qn < q.length; qn++) {
                    sigmaQ += q_w.get(qn) * q_w.get(qn);
                }
                q_term_size = Math.sqrt(sigmaQ);
            }
            Double bottom = Math.sqrt(q.length) * Math.sqrt(sigmad2);
            RankTuple rt = new RankTuple(document_id, (qdotd / (bottom * q_term_size)));
            result.add(rt);
        }
//        System.out.println(result);
//        System.out.println(result.size());
        return result;
    }

    private PriorityQueue<RankTuple> calc_rank_ratio(String[] q, List<Double> q_w, Comparator<RankTuple> tc) {
        PriorityQueue<RankTuple> result = new PriorityQueue<>(tc);
        if (q.length == 2) {
            HashMap<Integer, Double> common = intersect(thedictionary.get(q[0]), thedictionary.get(q[1]));
            result.addAll(common.entrySet().stream().map(entry -> new RankTuple(entry.getKey(), Math.abs(entry.getValue()
                    - (q_w.get(0) / q_w.get(1))))).collect(Collectors.toList()));
        } else {
            HashMap<Integer, Tuple<Double, Integer, Integer>> tempRatioHolder = new HashMap<>();
            for (int i = 0; i < q.length - 1; i++) {
                System.out.println(q[i] + " " + q[i + 1]);
                HashMap<Integer, Double> common = intersect(thedictionary.get(q[i]), thedictionary.get(q[i + 1]));
                for (Map.Entry<Integer, Double> entry : common.entrySet()) {
                    if (tempRatioHolder.containsKey(entry.getKey())) {
                        Tuple<Double, Integer, Integer> t = tempRatioHolder.get(entry.getKey());
//                        Double prev_sum = (t.x*t.y);
                        t.x = t.x + (1 / (0.01 + Math.abs(entry.getValue() - Math.abs(q_w.get(i) / q_w.get(i + 1)))));
                        tempRatioHolder.put(entry.getKey(), t);
                        System.out.println(entry.getKey());
                    } else
                        tempRatioHolder.put(entry.getKey(), new Tuple<>((1 / (0.01 + Math.abs(entry.getValue() -
                                Math.abs(q_w.get(i) / q_w.get(i + 1))))), 1, 1));
                }
            }
            //noinspection SuspiciousNameCombination
            result.addAll(tempRatioHolder.entrySet().stream().map(entry -> new RankTuple(entry.getKey(),
                    entry.getValue().x)).collect(Collectors.toList()));
        }
//        System.out.println(result);
        return result;

    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Breaks the main II Into num_of_slices tree maps to write them to a file
    private void disk_handler() throws IOException {
        FileWriter fw = new FileWriter("/home/erfan/Documents/programming/invertedIndex/dataset/InvertedIndex/src/com/company/storage/ii" + 1 + ".xml");
        fw.write("<INVERTED_INDEX>\n");
        Integer num_of_slices = 1;
        TreeMap<String, LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>>> treemap1 = new TreeMap<>();
        LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> templl;
        String[] kset = new String[thedictionary.size()];
        thedictionary.keySet().toArray(kset);
        for (String akey : kset) {
            if (treemap1.size() < thedictionary.size() / num_of_slices) {
                templl = thedictionary.get(akey);
                treemap1.put(akey, templl);
                thedictionary.remove(akey);
            }
            write_to_file(num_of_slices, treemap1, fw);
            treemap1.clear();
        }
        fw.write("</INVERTED_INDEX>\n");

    }

    // Gets a  tree map and attempts to write it to a file
    private static void write_to_file(Integer slices, TreeMap<String, LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>>> tm, FileWriter fw) throws IOException {
        for (int slice = 1; slice <= slices; slice++) {
            String k;
            LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> templl;
            while (!tm.isEmpty()) {
                k = tm.firstKey();
                fw.write("<KEY>\n");
                fw.write("<WORD>");
                fw.write(k);
                fw.write("</WORD>");

                fw.write("\n");
                templl = tm.get(k);
                fw.write("<POSTING_LIST>\n");
                for (Tuple<Integer, Integer, ArrayList<Integer>> t : templl) {
                    fw.write("<DOC id=\"" + t.x.toString() + "\" tf=\"" + t.y.toString() + "\">\n");
                    fw.write("<POSITIONS>\n");
                    for (Integer position : t.z) {
                        fw.write(position.toString() + " ");
                    }
                    fw.write("</POSITIONS>\n");
                    fw.write("</DOC>\n");
                }
                fw.write("</POSTING_LIST>\n");
                fw.write("/<KEY>\n");
                tm.remove(k);

            }

        }
    }

    @SuppressWarnings("unused")
    private void read_from_file() {
        try {

            File fXmlFile = new File("/home/erfan/IDE/IdeaProjects/InvertedIndex/src/com/company/storage/ll1.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();

            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

            NodeList nList = doc.getElementsByTagName("KEY");

            System.out.println("----------------------------");

            for (int temp = 0; temp < nList.getLength(); temp++) {

                Node nNode = nList.item(temp);

//                System.out.println("\nCurrent Element :" + nNode.getNodeName());

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                    Element eElement = (Element) nNode;

                    String key = eElement.getElementsByTagName("WORD").item(0).getTextContent();
                    LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> templl = new LinkedList<>();
                    NodeList node_postinglist = doc.getElementsByTagName("POSTING_LIST");
                    for (int j = 0; j < nList.getLength(); j++) {
                        Node tpl = node_postinglist.item(j);
                        Element tpl1 = (Element) tpl;
                        Tuple<Integer, Integer, ArrayList<Integer>> t = new Tuple<>(Integer.parseInt(tpl1.getAttribute("id")), Integer.parseInt(tpl1.getAttribute("tf")), new ArrayList<Integer>());
                        templl.add(t);
                    }
                    thedictionary.put(key, templl);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /****************************************************HELPERS*******************************************************/

    private HashMap<Integer, Double> intersect(LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> l1, LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> l2) {
        HashMap<Integer, Double> resultList = new HashMap<>();
        int pIndex1 = 0;
        int pIndex2 = 0;
        Tuple<Integer, Integer, ArrayList<Integer>> pointer1 = l1.get(pIndex1);
        Tuple<Integer, Integer, ArrayList<Integer>> pointer2 = l2.get(pIndex2);
        boolean finished = false;
        while (!finished) {
            if (pointer1.x.equals(pointer2.x)) {
//                System.out.println("doc: " + pointer1.x + " tf1: " + pointer1.y + " ft2: " + pointer2.y);
                resultList.put(pointer1.x, (pointer1.y * 1.0) / pointer2.y);
                if (pIndex1 < l1.size() - 1)
                    pointer1 = l1.get(++pIndex1);
                else
                    finished = true;
                if (pIndex2 < l2.size() - 1)
                    pointer2 = l2.get(++pIndex2);
                else
                    finished = true;
            } else if (pointer1.x > pointer2.x) {
                if (pIndex1 < l1.size() - 1)
                    pointer1 = l1.get(++pIndex1);
                else
                    finished = true;
            } else {
                if (pIndex2 < l2.size() - 1)
                    pointer2 = l2.get(++pIndex2);
                else
                    finished = true;
            }
        }
//        System.out.println(resultList);
        return resultList;
    }

    @SuppressWarnings({"Duplicates", "unused"})
    private HashMap<Integer, Double> union(LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> l1, LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> l2) {
        HashMap<Integer, Double> resultList = new HashMap<>();
        int pIndex1 = 0;
        int pIndex2 = 0;
        Tuple<Integer, Integer, ArrayList<Integer>> pointer1 = l1.get(pIndex1);
        Tuple<Integer, Integer, ArrayList<Integer>> pointer2 = l2.get(pIndex2);
        boolean finished1 = false;
        boolean finished2 = false;
        while (!finished1 && !finished2) {
            if (pointer1.x.equals(pointer2.x)) {
                resultList.put(pointer1.x, ((pointer1.y * 1.0) + 0.9) / (pointer2.y + 0.9));
                if (pIndex1 < l1.size() - 1)
                    pointer1 = l1.get(++pIndex1);
                else
                    finished1 = true;
                if (pIndex2 < l2.size() - 1)
                    pointer2 = l2.get(++pIndex2);
                else
                    finished2 = true;
            } else if (pointer1.x > pointer2.x) {
                if (pIndex1 < l1.size() - 1) {
                    resultList.put(pointer1.x, (pointer1.y + 0.9) / 0.9);
                    pointer1 = l1.get(++pIndex1);
                } else
                    finished1 = true;
            } else {
                if (pIndex2 < l2.size() - 1) {
                    resultList.put(pointer2.x, 0.9 / (pointer2.y + 0.9));
                    pointer2 = l2.get(++pIndex2);
                } else
                    finished2 = true;
            }
        }
        while (!finished1) {
            if (pIndex1 < l1.size() - 1) {
                resultList.put(pointer1.x, (pointer1.y + 0.9) / 0.9);
                pointer1 = l1.get(++pIndex1);
            } else
                finished1 = true;
        }
        while (!finished2) {
            if (pIndex2 < l2.size() - 1) {
                resultList.put(pointer2.x, 0.9 / (pointer2.y + 0.9));
                pointer2 = l2.get(++pIndex2);
            } else
                finished2 = true;
        }
//        System.out.println(resultList);
        return resultList;
    }

    private ArrayList<Integer> unionPostings(ArrayList<LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>>> postingLists) {
        ArrayList<Integer> result = new ArrayList<>();
        ArrayList<ArrayList<Integer>> postingListsCpy = new ArrayList<>();

        int totalSize = 0; // every element in the set
        for (LinkedList<Tuple<Integer, Integer, ArrayList<Integer>>> l : postingLists) {
            totalSize += l.size();
            ArrayList<Integer> list = l.stream().map(t -> t.x).collect(Collectors.toCollection(ArrayList::new));
            postingListsCpy.add(list);
        }
        ArrayList<Integer> lowest;
        Boolean[] flags = new Boolean[postingLists.size()];
        for(int i =0 ; i< flags.length; i++)
            flags[i] = false;
        Boolean done = false;

        while (result.size() < totalSize && !done) { // while we still have something to add
            lowest = null;

            for (int i=0; i< postingLists.size(); i++) {
                ArrayList<Integer> l = postingListsCpy.get(i);
                if (! l.isEmpty()) {
                    if (lowest == null) {
                        lowest = l;
                    } else if (l.get(0) <= lowest.get(0)) {
                        lowest = l;
                    }
                }
                else{
                    flags[i] = true;
                    if(check_finished(flags))
                        done = true;
                }
            }

            assert lowest != null;
            result.add(lowest.get(0));
            lowest.remove(0);
        }

        return result;
    }

    private Boolean check_finished(Boolean[] flags) {
        Boolean done = true;
        for (Boolean flag : flags){
            if(!flag){
                done = false;
                break;
            }
        }
     return done;
    }

}