<?php

namespace App {
    class NotAModel
    {
        protected ?string $connection = 'missing';

        protected $table = 'wrongTable';
    }
}
