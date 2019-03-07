(function () {
    let alphabetClassName = 'alphabet-link';
    let authorClassName = 'author-link';
    let bookClassName = 'book-link';
    let foundedBookClassName = 'book-link searched';
    let selectedBookClassName = 'book-link searched selected';
    let bookActionClassName = 'book-action-link';
    let bookSeriesClassName = 'book-series-link';
    let bookGenreClassName = 'book-genre-link';
    let classHidden = 'hidden';
    let forumNamesClassName = 'forum-name-link';

    function handleLinks(element) {
        /*// найду все ссылки*/
        let links = element.getElementsByTagName('A');

        if (links && links.length > 0) {
            /*// составлю регулярные выражения, по которым буду отличать ссылки друг от друга*/
            let alphabetListRegex = /^\/\w{1,2}$/;
            let authorRegex = /^\/a\/\d+$/;
            let bookRegex = /^\/b\/\d+$/;
            let bookSeriesRegex = /^\/s\/\d+$/;
            let bookGenreRegex = /^\/g\/[\d_\w]+$/;
            let bookActonRegex = /^\/b\/\d+\/\w+$/;
            let forumNamesRegex = /^\/polka\/show\/[\d]+$/;


            let counter = 0;
            let href;
            while (links[counter]) {
                if(links[counter].offsetHeight){
                    href = links[counter].getAttribute('href');
                    if (bookRegex.test(href)) {
                        links[counter].className = bookClassName;
                    }
                    else if (bookActonRegex.test(href)) {
                        /*// похоже на действие с книгой, удалю скобки*/
                        links[counter].className = bookActionClassName;
                        links[counter].innerText = links[counter].innerText.replace(/[()]/g, '');
                    }
                    /*// проверю на соответствие классу автора*/
                    else if (authorRegex.test(href)) {
                        links[counter].className = authorClassName;
                    }
                    else if (bookSeriesRegex.test(href)) {
                        links[counter].className = bookSeriesClassName;
                    }
                    else if (bookGenreRegex.test(href)) {
                        links[counter].className = bookGenreClassName;
                    }
                    else if (links[counter].innerText === '(СЛЕДИТЬ)') {
                        links[counter].className = classHidden;
                    }
                    /*// проверю на соответствие классу ссылок на алфавит*/
                    else if (alphabetListRegex.test(href) || href === '/a/all' || href === '/Other') {
                        links[counter].className = alphabetClassName;
                        links[counter].innerText = links[counter].innerText.replace(/[\[\]]/g, '');
                    }
                    else if (forumNamesRegex.test(href)) {
                        links[counter].className = forumNamesClassName;
                    }
                }
                counter++;
            }
        }
    }

    handleLinks(document);
    /*// отслеживаю загрузку книг в выбранный раздел*/

    /*// выбираем целевой элемент*/
    let target = document.getElementById('books');
    if (target) {
        /*// создаём экземпляр MutationObserver*/
        let observer = new MutationObserver(function () {
            handleLinks(target);
        });

        /*// конфигурация нашего observer:*/
        let config = {attributes: true, childList: true, characterData: true};

        /*// передаём в качестве аргументов целевой элемент и его конфигурацию*/
        observer.observe(target, config);

        /*// позже можно остановить наблюдение
                //observer.disconnect();*/
    }


    let books = document.getElementsByClassName(bookClassName);
    if(books && books.length > 0)
    /*    // если на странице найдены книги- добавляю строку поиска по названиям*/
        if (books && books.length > 0) {
            let searchDiv, searchButton, searchField;

            searchDiv = document.createElement('div');
            searchDiv.id = 'searchContainer';


            searchField = document.createElement('input');
            searchField.type = 'text';
            searchField.id = 'booksSearcher';
            searchField.setAttribute('placeholder', 'Искать книгу на странице');


            searchButton = document.createElement('div');
            searchButton.id = 'searchButton';
            let innerText = 'Нет условия';
            searchButton.innerText = innerText;

            searchDiv.appendChild(searchField);
            searchDiv.appendChild(searchButton);
            document.body.appendChild(searchDiv);

            let founded;
            let searchShift = 0;
            let previouslySelected;
            let previousSearch;

            function makeBookSearched(foundedElement) {
                /*            // сначала сброшу выделение с ранее найденных компонентов*/
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
                if(previouslySelected){
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

            searchField.onkeypress = function (event) {
                if (event.code === 'Enter') {
                    switchToNext();
                }
            };

            searchField.oninput = function () {
                let inputVal = searchField.value.toLowerCase();
                if (inputVal) {
                    founded = [];
                    searchShift = 0;
                    let i = 0;
                    while (books[i]) {
                        if (books[i].innerText.toLowerCase().indexOf(inputVal) + 1 && books[i].offsetHeight) {
                            founded.push(books[i]);
                        }
                        i++;
                    }
                    if (founded && founded.length > 0) {
                        // перехожу к первой найденной книге
                        scrollTo(founded[0]);
                        searchButton.innerText = '1 из ' + founded.length;
                        makeBookSearched(founded);
                        makeSearchedBookSelected(founded[0]);
                    }
                    else {
                        clearSelectedItem();
                        clearSearchedItems();
                        searchButton.innerText = 'Не найдено';
                    }
                }
                else {
                    clearSelectedItem();
                    clearSearchedItems();
                    searchButton.innerText = innerText;
                    founded = [];
                }

            };
            searchButton.onclick = function () {
                switchToNext();
            };

            function switchToNext() {
                if (founded && founded.length > 0) {
                    ++searchShift;
                    if (founded[searchShift]) {
                        scrollTo(founded[searchShift]);
                        searchButton.innerText = (searchShift + 1) + ' из ' + founded.length;
                        makeSearchedBookSelected(founded[searchShift]);
                    }
                    else {
                        searchShift = 0;
                        scrollTo(founded[0]);
                        searchButton.innerText = '1 из ' + founded.length;
                        makeSearchedBookSelected(founded[0]);
                    }
                }
            }

            function scrollTo(element) {
                let offset = element.offsetTop - 10;
                window.scroll(0, offset - 10);
            }
        }
}());