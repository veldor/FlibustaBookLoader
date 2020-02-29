async function handle(){
var navigationButtonsDiv = document.createElement('div');
    navigationButtonsDiv.id = 'navigationButtonsDiv';
    navigationButtonsDiv.innerHTML = "<button id='moveUpBtn'>Вверх</button><button id='moveDownBtn'>Вниз</button>";
    document.body.appendChild(navigationButtonsDiv);

    var upBtn = document.getElementById('moveUpBtn');
    var downBtn = document.getElementById('moveDownBtn');

    upBtn.onclick = function(){
        window.scrollTo(0, 0);
    }
    downBtn.onclick = function(){
        window.scrollTo(0,document.body.scrollHeight);
    }

    var href = location.href;
    if (href === 'http://flibustahezeous3.onion/') {
        var menu = document.getElementsByClassName('pager');
        menu[0].style.display = 'none';
    }
    var alphabetClassName = 'alphabet-link';
    var authorClassName = 'author-link';
    var bookClassName = 'book-link';
    var foundedBookClassName = 'book-link searched';
    var selectedBookClassName = 'book-link searched selected';
    var bookActionClassName = 'book-action-link';
    var bookSeriesClassName = 'book-series-link';
    var bookGenreClassName = 'book-genre-link';
    var classHidden = 'hidden';
    var forumNamesClassName = 'forum-name-link';
    handleLinks(document);
    var target = document.getElementById('books');
    if (target) {
        var observer = new MutationObserver(function () {
            handleLinks(target);
        });
        var config = {attributes: true, childList: true, characterData: true};
        observer.observe(target, config);
    }
    var books = document.getElementsByClassName(bookClassName);
    if (books && books.length > 0) if (books && books.length > 0) {
        var searchDiv, searchButton, searchField;
        searchDiv = document.createElement('div');
        searchDiv.id = 'searchContainer';
        searchField = document.createElement('input');
        searchField.type = 'text';
        searchField.id = 'booksSearcher';
        searchField.setAttribute('placeholder', 'Искать книгу на странице');
        searchButton = document.createElement('div');
        searchButton.id = 'searchButton';
        var innerText = 'Нет условия';
        searchButton.innerText = innerText;
        searchDiv.appendChild(searchField);
        searchDiv.appendChild(searchButton);
        document.body.appendChild(searchDiv);
        var founded;
        var searchShift = 0;
        var previouslySelected;
        var previousSearch;
        searchField.onkeypress = function (event) {
            if (event.code === 'Enter') {
                switchToNext();
            }
        };
        searchField.oninput = function () {
            var inputVal = searchField.value.toLowerCase();
            if (inputVal) {
                founded = [];
                searchShift = 0;
                var i = 0;
                while (books[i]) {
                    if (books[i].innerText.toLowerCase().indexOf(inputVal) + 1 && books[i].offsetHeight) {
                        founded.push(books[i]);
                    }
                    i++;
                }
                if (founded && founded.length > 0) {
                    scrollTo(founded[0]);
                    searchButton.innerText = '1 из ' + founded.length;
                    makeBookSearched(founded);
                    makeSearchedBookSelected(founded[0]);
                } else {
                    clearSelectedItem();
                    clearSearchedItems();
                    searchButton.innerText = 'Не найдено';
                }
            } else {
                clearSelectedItem();
                clearSearchedItems();
                searchButton.innerText = innerText;
                founded = [];
            }
        };
        searchButton.onclick = function () {
            switchToNext();
        };
    }

    function makeBookSearched(foundedElement) {
        if (previousSearch && previousSearch.length > 0) {
            previousSearch.forEach(function (elem) {
                elem.className = bookClassName;
            });
        }
        if (foundedElement && foundedElement.length > 0) {
            foundedElement.forEach(function (elem) {
                elem.className = foundedBookClassName;
            });
            previousSearch = foundedElement;
        }
    }

    function makeSearchedBookSelected(elem) {
        if (previouslySelected) {
            previouslySelected.className = foundedBookClassName;
        }
        elem.className = selectedBookClassName;
        previouslySelected = elem;
    }

    function clearSelectedItem() {
        if (previouslySelected) {
            previouslySelected.className = bookClassName;
            previouslySelected = null;
        }
    }

    function clearSearchedItems() {
        if (previousSearch && previousSearch.length > 0) {
            previousSearch.forEach(function (elem) {
                elem.className = bookClassName;
            });
            previousSearch = null;
        }
    }

    function switchToNext() {
        if (founded && founded.length > 0) {
            ++searchShift;
            if (founded[searchShift]) {
                scrollTo(founded[searchShift]);
                searchButton.innerText = (searchShift + 1) + ' из ' + founded.length;
                makeSearchedBookSelected(founded[searchShift]);
            } else {
                searchShift = 0;
                scrollTo(founded[0]);
                searchButton.innerText = '1 из ' + founded.length;
                makeSearchedBookSelected(founded[0]);
            }
        }
    }

    function scrollTo(element) {
        var offset = element.offsetTop - 10;
        window.scroll(0, offset - 10);
    }

    function handleLinks(element) {
        var links = element.getElementsByTagName('A');
        if (links && links.length > 0) {
            var alphabetListRegex = /^\/\w{1,2}$/;
            var authorStarts = '/a/';
            var bookStarts = '/b/';
            var seriesStarts = '/s/';
            var genreStarts = '/g/';
            var forumNamesRegex = /^\/polka\/show\/[\d]$/;
            var counter = 0;
            var href;
            var startsWith;
            var prelast;
            var current;
            while (links[counter]) {
                current = links[counter];
                if (current.offsetHeight) {
                    href = current.getAttribute('href');
                    startsWith = href.substr(0, 3);
                    if (startsWith === bookStarts) {
                        prelast = parseInt(href.substr(href.length - 2, 1) + 1);
                        if (prelast) {
                            current.className = bookClassName;
                        } else {
                            current.className = bookActionClassName;
                            current.innerText = links[counter].innerText.replace(/[()]/g, '');
                        }
                    } else if (startsWith === authorStarts) {
                        if (href === '/a/all') {
                            current.className = alphabetClassName;
                        } else {
                            current.className = authorClassName;
                        }
                    } else if (startsWith === genreStarts) {
                        current.className = bookGenreClassName;
                    } else if (startsWith === seriesStarts) {
                        current.className = bookSeriesClassName;
                    } else if (current.innerText === '(СЛЕДИТЬ)') {
                        current.className = classHidden;
                    } else if (alphabetListRegex.test(href) || href === '/a/all' || href === '/Other') {
                        current.className = alphabetClassName;
                        current.innerText = current.innerText.replace(/[\[\]]/g, '');
                    } else if (forumNamesRegex.test(href)) {
                        current.className = forumNamesClassName;
                    }
                }
                counter++;
            }
        }
    }
}

$(window).on("load.my", function () {
    handle();
});