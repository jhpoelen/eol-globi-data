package org.trophic.graph.domain;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.trophic.graph.domain.Movie;
import org.trophic.graph.domain.Person;
import org.trophic.graph.domain.Rating;
import org.trophic.graph.domain.Role;
import org.trophic.graph.domain.User;
import org.trophic.graph.repository.MovieRepository;
import org.trophic.graph.repository.UserRepository;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author mh
 * @since 04.03.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/movies-test-context.xml"})
@Transactional
public class DomainTest {

    @Autowired
    protected MovieRepository movieRepository;
    @Autowired
    protected UserRepository userRepository;

    @Test
    public void actorCanPlayARoleInAMovie() {
        Person tomHanks = new Person("1","Tom Hanks").persist();
        Movie forestGump = new Movie("1", "Forrest Gump").persist();

        Role role = tomHanks.playedIn(forestGump, "Forrest");

        Movie foundForestGump = this.movieRepository.findByPropertyValue("id", "1");

        assertEquals("created and looked up movie equal", forestGump, foundForestGump);
        Role firstRole = foundForestGump.getRoles().iterator().next();
        assertEquals("role forrest",role, firstRole);
        assertEquals("role forrest","Forrest", firstRole.getName());
    }

    @Test
    public void canFindMovieByTitleQuery() {
        Movie forestGump = new Movie("1", "Forrest Gump").persist();
        Iterator<Movie> queryResults = movieRepository.findAllByQuery("search", "title", "Forre*").iterator();
        assertTrue("found movie by query",queryResults.hasNext());
        Movie foundMovie = queryResults.next();
        assertEquals("created and looked up movie equal", forestGump, foundMovie);
        assertFalse("found only one movie by query", queryResults.hasNext());
    }

    @Test
    public void userCanRateMovie() {
        Movie movie= new Movie("1","Forrest Gump").persist();
        User user = new User("ich","Micha","password").persist();
        Rating awesome = user.rate(movie, 5, "Awesome");


        User foundUser = userRepository.findByPropertyValue("login", "ich");
        Rating rating = user.getRatings().iterator().next();
        assertEquals(awesome,rating);
        assertEquals("Awesome",rating.getComment());
        assertEquals(5,rating.getStars());
        assertEquals(5,movie.getStars(),0);
    }
    @Test
    public void canFindUserByLogin() {
        User user = new User("ich","Micha","password").persist();
        User foundUser = userRepository.findByPropertyValue("login", "ich");
        assertEquals(user, foundUser);
    }
}
