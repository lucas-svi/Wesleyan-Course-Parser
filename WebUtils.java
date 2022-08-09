import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebUtils {
    private static final String baseURL = "https://owaprod-pub.wesleyan.edu/reg/";
    private static final String wesMapsURL = "!wesmaps_page.html?stuid=&facid=NONE&term=1229";
    public static List<Object[]> initialLinks = new ArrayList<>();
    public static List<Course> courses = new ArrayList<>();

    public static boolean initialParsing = false;

    //private static List<>
    public static void parseWesleyanSite() throws Exception {
        int counter = 0;
        if (initialLinks.size() != 0) {
            for (Object[] link : initialLinks) {
                if (counter >= 5) {
                    for (Object[] linksInCourse : getClickables(getDoc((String) link[0]))) {
                        if (((String) linksInCourse[1]).contains("Courses Offered")) {
                            List<Object[]> test = getClickables(getDoc(linksInCourse[0].toString()));
                            for (Object[] objects : test) {
                                String[] split = objects[1].toString().split("-");
                                if (split.length == 2) {
                                    if (!courses.contains(Course.getCourseFromNameAndSection(courses, String.valueOf(split[0]), Integer.parseInt(split[1])))) {
                                        courses.add(new Course(String.valueOf(split[0]), Integer.parseInt(split[1])));
                                        //Handler.sendMessage(Utils.getChannel("1006047535636434955"), split[0] + " Section: " + split[1] + " Seats Available: " + findAvailableSeats(getDoc((String) objects[0])));
                                    }
                                }
                            }
                        }
                    }
                }
                counter++;
            }
            initialParsing = true;
        }
    }

    public static void parseWesleyanSite(String courseName, int section, SlashCommandEvent event) throws Exception {
        int counter = 0;
        if (initialParsing) {
            if (Course.getCourseFromNameAndSection(courses, courseName, section) == null) {
                Handler.sendPrivate(event.getUser(), "Was unable to find " + courseName + "-" + section + ", " + event.getMember().getAsMention() + "!");
            } else {
                Handler.sendPrivate(event.getUser(), "Your course has been found in my database, please be patient as I look for it, " + event.getMember().getAsMention() + "!");
                if (initialLinks.size() != 0) {
                    loop:
                    for (Object[] link : initialLinks) {
                        if (counter >= 5) {
                            for (Object[] linksInCourse : getClickables(getDoc((String) link[0]))) {
                                if (((String) linksInCourse[1]).contains("Courses Offered")) {
                                    List<Object[]> test = getClickables(getDoc(linksInCourse[0].toString()));
                                    for (Object[] objects : test) {
                                        String[] split = objects[1].toString().split("-");
                                        if (split.length == 2) {
                                            if (String.valueOf(split[0]).equalsIgnoreCase(courseName) && Integer.parseInt(split[1]) == section) {
                                                Handler.sendPrivate(event.getMember().getUser(), split[0] + " Section: " + split[1] + " Seats Available: " + findAvailableSeats(getDoc((String) objects[0])));
                                                break loop;
                                            }
                                            //System.out.println(split[0] + " " + Integer.parseInt(split[1]));
                                        }
                                    }
                                }
                            }
                        }
                        counter++;
                    }
                }
            }
        } else {
            Handler.sendPrivate(event.getUser(), "Initial parsing isn't complete! Please wait a bit before trying again, please?");
        }
    }

    public static List<Object[]> getClickables(Document doc) {
        List<Object[]> list = new ArrayList<>();
        Elements links = doc.select("a[href]");
        for (Element e : links) {
            String attribute = e.attr("href");
            if (attribute.contains("!wesmaps_page") && !attribute.contains("FORM")) {
                //System.out.println("\nlink : " + attribute + " " + initialLinks.size());
                //System.out.println("text : " + e.text());
                if (!(e.text().equals("Summer Session") || e.text().equals("Winter Session") || e.text().equals("Home") || e.text().equals("Archive") || e.text().equals("Search") || e.text().equals("NONS"))) {
                    list.add(new Object[]{attribute /*link*/, e.text() /*name of link*/});
                }
            }
        }
        return list;
    }

    public static Document getDoc(String URL) throws IOException {
        return Jsoup.connect(baseURL + URL).get();
    }

    public static void initWesleyanParsing() throws Exception {
        initialLinks = getClickables(getDoc(wesMapsURL));
        System.out.println(initialLinks.size() + " size of mainpage");
        parseWesleyanSite();
    }

    public static int findAvailableSeats(Document doc) {
        String text = doc.text();
        String s = "Seats Available: ";
        if (text.contains(s)) {
            String substring = text.substring(text.indexOf(s) + s.length(), text.indexOf(s) + s.length() + 2);
            return Integer.parseInt(substring.replaceAll(" ", ""));
        }
        return -1;
    }
}
